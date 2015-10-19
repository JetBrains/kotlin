/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.MultiRequestPositionManager
import com.intellij.debugger.NoDataException
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.*
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.JetTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.fileClasses.getFileClassInternalName
import org.jetbrains.kotlin.fileClasses.getInternalName
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.decompiler.JetClsFile
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.util.DebuggerUtils
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.util.*
import com.intellij.debugger.engine.DebuggerUtils as JDebuggerUtils

class PositionedElement(val className: String?, val element: PsiElement?)

public class JetPositionManager(private val myDebugProcess: DebugProcess) : MultiRequestPositionManager {

    private val myTypeMappers = WeakHashMap<String, CachedValue<JetTypeMapper>>()

    override fun getSourcePosition(location: Location?): SourcePosition? {
        if (location == null) {
            throw NoDataException.INSTANCE
        }

        val psiFile = getPsiFileByLocation(location)
        if (psiFile == null) {
            val isKotlinStrataAvailable = location.declaringType().availableStrata().contains("Kotlin")
            if (isKotlinStrataAvailable) {
                try {
                    val javaSourceFileName = location.sourceName("Java")
                    val javaClassName = JvmClassName.byInternalName(defaultInternalName(location))
                    val project = myDebugProcess.project

                    val defaultPsiFile = DebuggerUtils.findSourceFileForClass(project, GlobalSearchScope.allScope(project), javaClassName, javaSourceFileName)
                    if (defaultPsiFile != null) {
                        return SourcePosition.createFromLine(defaultPsiFile, 0)
                    }
                }
                catch(e: AbsentInformationException) {
                    // ignored
                }
            }

            throw NoDataException.INSTANCE
        }

        val lineNumber = try {
            location.lineNumber() - 1
        }
        catch (e: InternalError) {
            -1
        }


        if (lineNumber >= 0) {
            val lambdaOrFunIfInside = getLambdaOrFunIfInside(location, psiFile as KtFile, lineNumber)
            if (lambdaOrFunIfInside != null) {
                return SourcePosition.createFromElement(lambdaOrFunIfInside.bodyExpression!!)
            }
            return SourcePosition.createFromLine(psiFile, lineNumber)
        }

        throw NoDataException.INSTANCE
    }

    private fun getLambdaOrFunIfInside(location: Location, file: KtFile, lineNumber: Int): KtFunction? {
        val currentLocationFqName = location.declaringType().name()
        if (currentLocationFqName == null) return null

        val start = CodeInsightUtils.getStartLineOffset(file, lineNumber)
        val end = CodeInsightUtils.getEndLineOffset(file, lineNumber)
        if (start == null || end == null) return null

        val literalsOrFunctions = CodeInsightUtils.
                findElementsOfClassInRange(file, start, end, KtFunctionLiteral::class.java, KtNamedFunction::class.java).
                filter { KtPsiUtil.getParentCallIfPresent(it as KtExpression) != null }

        if (literalsOrFunctions.isEmpty()) return null;

        val isInLibrary = LibraryUtil.findLibraryEntry(file.virtualFile, file.project) != null
        val typeMapper = if (!isInLibrary)
            prepareTypeMapper(file)
        else
            createTypeMapperForLibraryFile(file.findElementAt(start), file)

        val currentLocationClassName = JvmClassName.byFqNameWithoutInnerClasses(FqName(currentLocationFqName)).internalName
        for (literal in literalsOrFunctions) {
            val functionLiteral = literal as KtFunction
            if (isInlinedLambda(functionLiteral, typeMapper.bindingContext)) {
                continue
            }

            val internalClassName = getInternalClassNameForElement(literal.firstChild, typeMapper, file, isInLibrary).className
            if (internalClassName == currentLocationClassName) {
                return functionLiteral
            }
        }

        return null
    }

    private fun getPsiFileByLocation(location: Location): PsiFile? {
        val sourceName: String
        try {
            sourceName = location.sourceName()
        }
        catch (e: AbsentInformationException) {
            return null
        }
        catch (e: InternalError) {
            return null
        }


        val referenceInternalName: String
        try {
            if (location.declaringType().availableStrata().contains("Kotlin")) {
                //replace is required for windows
                referenceInternalName = location.sourcePath().replace('\\','/')
            } else {
                referenceInternalName = defaultInternalName(location)
            }
        }
        catch (e: AbsentInformationException) {
            referenceInternalName = defaultInternalName(location)
        }

        val className = JvmClassName.byInternalName(referenceInternalName)

        val project = myDebugProcess.project

        return DebuggerUtils.findSourceFileForClass(project, GlobalSearchScope.allScope(project), className, sourceName)
    }

    private fun defaultInternalName(location: Location): String {
        //no stratum or source path => use default one
        val referenceFqName = location.declaringType().name()
        // JDI names are of form "package.Class$InnerClass"
        return referenceFqName.replace('.', '/')
    }

    override fun getAllClasses(sourcePosition: SourcePosition): List<ReferenceType> {
        val psiFile = sourcePosition.file
        if (psiFile is KtFile) {
            val result = ArrayList<ReferenceType>()

            if (!ProjectRootsUtil.isInProjectOrLibSource(psiFile)) return result

            val names = classNameForPositionAndInlinedOnes(sourcePosition)
            for (name in names) {
                result.addAll(myDebugProcess.virtualMachineProxy.classesByName(name))
            }
            return result
        }

        if (psiFile is ClsFileImpl) {
            val decompiledPsiFile = runReadAction { psiFile.decompiledPsiFile }
            if (decompiledPsiFile is JetClsFile && sourcePosition.line == -1) {
                val className = JvmFileClassUtil.getFileClassInfoNoResolve(decompiledPsiFile).fileClassFqName.getInternalName()
                return myDebugProcess.virtualMachineProxy.classesByName(className)
            }
        }

        throw NoDataException.INSTANCE
    }

    private fun classNameForPositionAndInlinedOnes(sourcePosition: SourcePosition): List<String> {
        val result = arrayListOf<String>()
        val name = classNameForPosition(sourcePosition)
        if (name != null) {
            result.add(name)
        }
        val list = findInlinedCalls(sourcePosition.elementAt, sourcePosition.file)
        result.addAll(list)

        return result;
    }

    public fun classNameForPosition(sourcePosition: SourcePosition): String? {
        val psiElement = runReadAction { sourcePosition.elementAt } ?: return null
        return classNameForPosition(psiElement)
    }

    private fun classNameForPosition(element: PsiElement): String? {
        return runReadAction {
            if (DumbService.getInstance(element.project).isDumb) {
                null
            }
            else {
                val file = element.containingFile as KtFile
                val isInLibrary = LibraryUtil.findLibraryEntry(file.virtualFile, file.project) != null
                val typeMapper = if (!isInLibrary) prepareTypeMapper(file) else createTypeMapperForLibraryFile(element, file)
                getInternalClassNameForElement(element, typeMapper, file, isInLibrary).className
            }
        }
    }

    private fun prepareTypeMapper(file: KtFile): JetTypeMapper {
        val key = createKeyForTypeMapper(file)

        var value: CachedValue<JetTypeMapper>? = myTypeMappers.get(key)
        if (value == null) {
            value = CachedValuesManager.getManager(file.project).createCachedValue<JetTypeMapper>(
                    {
                        val typeMapper = createTypeMapper(file)
                        CachedValueProvider.Result(typeMapper, PsiModificationTracker.MODIFICATION_COUNT)
                    }, false)

            myTypeMappers.put(key, value)
        }

        return value.value
    }

    override fun locationsOfLine(type: ReferenceType, position: SourcePosition): List<Location> {
        if (position.file !is KtFile) {
            throw NoDataException.INSTANCE
        }
        try {
            val line = position.line + 1
            val locations = if (myDebugProcess.virtualMachineProxy.versionHigher("1.4"))
                type.locationsOfLine("Kotlin", null, line)
            else
                type.locationsOfLine(line)
            if (locations == null || locations.isEmpty()) throw NoDataException.INSTANCE
            return locations
        }
        catch (e: AbsentInformationException) {
            throw NoDataException.INSTANCE
        }
    }

    @Deprecated("Since Idea 14.0.3 use createPrepareRequests fun")
    override fun createPrepareRequest(classPrepareRequestor: ClassPrepareRequestor, sourcePosition: SourcePosition): ClassPrepareRequest? {
        if (sourcePosition.file !is KtFile) {
            throw NoDataException.INSTANCE
        }
        val className = classNameForPosition(sourcePosition) ?: return null
        return myDebugProcess.requestsManager.createClassPrepareRequest(classPrepareRequestor, className.replace('/', '.'))
    }

    override fun createPrepareRequests(requestor: ClassPrepareRequestor, position: SourcePosition): List<ClassPrepareRequest> {
        if (position.file !is KtFile) {
            throw NoDataException.INSTANCE
        }

        return classNameForPositionAndInlinedOnes(position).map {
            className -> myDebugProcess.requestsManager.createClassPrepareRequest(requestor, className.replace('/', '.'))
        }.filterNotNull()
    }

    @TestOnly
    public fun addTypeMapper(file: KtFile, typeMapper: JetTypeMapper) {
        val value = CachedValuesManager.getManager(file.project).createCachedValue<JetTypeMapper>(
                { CachedValueProvider.Result(typeMapper, PsiModificationTracker.MODIFICATION_COUNT) }, false)
        val key = createKeyForTypeMapper(file)
        myTypeMappers.put(key, value)
    }

    companion object {
        public fun createTypeMapper(file: KtFile): JetTypeMapper {
            val project = file.project

            val analysisResult = file.analyzeFullyAndGetResult()
            analysisResult.throwIfError()

            val state = GenerationState(
                    project,
                    ClassBuilderFactories.THROW_EXCEPTION,
                    analysisResult.moduleDescriptor,
                    analysisResult.bindingContext,
                    listOf(file))
            state.beforeCompile()
            return state.typeMapper
        }

        public fun getInternalClassNameForElement(notPositionedElement: PsiElement?, typeMapper: JetTypeMapper, file: KtFile, isInLibrary: Boolean): PositionedElement {
            val element = getElementToCalculateClassName(notPositionedElement)
            when {
                element is KtClassOrObject -> return PositionedElement(getJvmInternalNameForImpl(typeMapper, element), element)
                element is KtFunctionLiteral -> {
                    if (isInlinedLambda(element, typeMapper.bindingContext)) {
                        return getInternalClassNameForElement(element.parent, typeMapper, file, isInLibrary)
                    }
                    else {
                        val asmType = CodegenBinding.asmTypeForAnonymousClass(typeMapper.bindingContext, element)
                        return PositionedElement(asmType.internalName, element)
                    }
                }
                element is KtClassInitializer -> {
                    val parent = getElementToCalculateClassName(element.parent)
                    // Class-object initializer
                    if (parent is KtObjectDeclaration && parent.isCompanion()) {
                        return PositionedElement(getInternalClassNameForElement(parent.parent, typeMapper, file, isInLibrary).className, parent)
                    }
                    return getInternalClassNameForElement(element.parent, typeMapper, file, isInLibrary)
                }
                element is KtProperty && (!element.isTopLevel || !isInLibrary) -> {
                    if (isInPropertyAccessor(notPositionedElement)) {
                        val classOrObject = PsiTreeUtil.getParentOfType(element, KtClassOrObject::class.java)
                        if (classOrObject != null) {
                            return PositionedElement(getJvmInternalNameForImpl(typeMapper, classOrObject), element)
                        }
                    }

                    val descriptor = typeMapper.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
                    if (descriptor !is PropertyDescriptor) {
                        return getInternalClassNameForElement(element.parent, typeMapper, file, isInLibrary)
                    }

                    return PositionedElement(getJvmInternalNameForPropertyOwner(typeMapper, descriptor), element)
                }
                element is KtNamedFunction -> {
                    if (isInlinedLambda(element, typeMapper.bindingContext)) {
                        return getInternalClassNameForElement(element.parent, typeMapper, file, isInLibrary)
                    }

                    val parent = getElementToCalculateClassName(element.parent)
                    if (parent is KtClassOrObject) {
                        return PositionedElement(getJvmInternalNameForImpl(typeMapper, parent), element)
                    }
                    else if (parent != null) {
                        val asmType = CodegenBinding.asmTypeForAnonymousClass(typeMapper.bindingContext, element)
                        return PositionedElement(asmType.internalName, element)
                    }
                }
            }

            return PositionedElement(NoResolveFileClassesProvider.getFileClassInternalName(file), element)
        }

        private val TYPES_TO_CALCULATE_CLASSNAME: Array<Class<out KtElement>> =
                arrayOf(KtClassOrObject::class.java,
                        KtFunctionLiteral::class.java,
                        KtNamedFunction::class.java,
                        KtProperty::class.java,
                        KtClassInitializer::class.java)

        private fun getElementToCalculateClassName(notPositionedElement: PsiElement?): KtElement? {
            if (notPositionedElement?.javaClass in TYPES_TO_CALCULATE_CLASSNAME ) return notPositionedElement as KtElement

            return PsiTreeUtil.getParentOfType(notPositionedElement, *TYPES_TO_CALCULATE_CLASSNAME)
        }

        public fun getJvmInternalNameForPropertyOwner(typeMapper: JetTypeMapper, descriptor: PropertyDescriptor): String {
            return typeMapper.mapOwner(
                    if (AsmUtil.isPropertyWithBackingFieldInOuterClass(descriptor)) descriptor.containingDeclaration else descriptor
            ).internalName
        }

        private fun isInPropertyAccessor(element: PsiElement?) =
                element is KtPropertyAccessor ||
                PsiTreeUtil.getParentOfType(element, KtProperty::class.java, KtPropertyAccessor::class.java) is KtPropertyAccessor

        private fun getElementToCreateTypeMapperForLibraryFile(element: PsiElement?) =
                if (element is KtElement) element else PsiTreeUtil.getParentOfType(element, KtElement::class.java)

        private fun getJvmInternalNameForImpl(typeMapper: JetTypeMapper, ktClass: KtClassOrObject): String? {
            val classDescriptor = typeMapper.bindingContext.get<PsiElement, ClassDescriptor>(BindingContext.CLASS, ktClass) ?: return null

            if (ktClass is KtClass && ktClass.isInterface()) {
                return typeMapper.mapDefaultImpls(classDescriptor).internalName
            }

            return typeMapper.mapClass(classDescriptor).internalName
        }

        private fun createTypeMapperForLibraryFile(notPositionedElement: PsiElement?, file: KtFile): JetTypeMapper {
            val element = getElementToCreateTypeMapperForLibraryFile(notPositionedElement)
            val analysisResult = element!!.analyzeAndGetResult()

            val state = GenerationState(file.project, ClassBuilderFactories.THROW_EXCEPTION, analysisResult.moduleDescriptor, analysisResult.bindingContext, listOf(file))
            state.beforeCompile()
            return state.typeMapper
        }

        public fun isInlinedLambda(functionLiteral: KtFunction, context: BindingContext): Boolean {
            return InlineUtil.isInlinedArgument(functionLiteral, context, false)
        }

        private fun createKeyForTypeMapper(file: KtFile) = NoResolveFileClassesProvider.getFileClassInternalName(file)
    }

    private fun findInlinedCalls(element: PsiElement?, jetFile: PsiFile?): List<String> {
        if (element == null || jetFile !is KtFile) {
            return emptyList()
        }

        return runReadAction {
            val result = arrayListOf<String>()
            val isInLibrary = LibraryUtil.findLibraryEntry(jetFile.virtualFile, jetFile.project) != null
            val typeMapper = if (!isInLibrary) prepareTypeMapper(jetFile) else createTypeMapperForLibraryFile(element, jetFile)
            val psiElement = getInternalClassNameForElement(element, typeMapper, jetFile, isInLibrary).element;

            if (psiElement is KtNamedFunction &&
                InlineUtil.isInline(typeMapper.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiElement))
            ) {
                ReferencesSearch.search(psiElement).forEach {
                    if (!it.isImportUsage()) {
                        val usage = it.element
                        if (usage is KtElement) {
                            //TODO recursive search
                            val name = classNameForPosition(usage)
                            if (name != null) {
                                result.add(name)
                            }
                        }
                    }
                }
            }
            result
        }
    }
}
