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

import com.google.common.collect.ObjectArrays
import com.intellij.debugger.MultiRequestPositionManager
import com.intellij.debugger.NoDataException
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.GlobalSearchScope
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
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.decompiler.JetClsFile
import org.jetbrains.kotlin.idea.findUsages.toSearchTarget
import org.jetbrains.kotlin.idea.search.usagesSearch.DefaultSearchHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.search
import org.jetbrains.kotlin.idea.util.DebuggerUtils
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineAnalyzerExtension
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.util.*

class PositionedElement(val className: String?, val element: PsiElement?)

public class JetPositionManager(private val myDebugProcess: DebugProcess) : MultiRequestPositionManager {
    private val myTypeMappers = WeakHashMap<String, CachedValue<JetTypeMapper>>()

    override fun getSourcePosition(location: Location?): SourcePosition? {
        if (location == null) {
            throw NoDataException.INSTANCE
        }
        val psiFile = getPsiFileByLocation(location)
        if (psiFile == null) {
            throw NoDataException.INSTANCE
        }

        val lineNumber = try {
            location.lineNumber() - 1
        }
        catch (e: InternalError) {
            -1
        }


        if (lineNumber >= 0) {
            val lambdaOrFunIfInside = getLambdaOrFunIfInside(location, psiFile as JetFile, lineNumber)
            if (lambdaOrFunIfInside != null) {
                return SourcePosition.createFromElement(lambdaOrFunIfInside.getBodyExpression())
            }
            return SourcePosition.createFromLine(psiFile, lineNumber)
        }

        throw NoDataException.INSTANCE
    }

    private fun getLambdaOrFunIfInside(location: Location, file: JetFile, lineNumber: Int): JetFunction? {
        val currentLocationFqName = location.declaringType().name()
        if (currentLocationFqName == null) return null

        val start = CodeInsightUtils.getStartLineOffset(file, lineNumber)
        val end = CodeInsightUtils.getEndLineOffset(file, lineNumber)
        if (start == null || end == null) return null

        val literalsOrFunctions = CodeInsightUtils.
                findElementsOfClassInRange(file, start, end, javaClass<JetFunctionLiteral>(), javaClass<JetNamedFunction>()).
                filter { JetPsiUtil.getParentCallIfPresent(it as JetExpression) != null }

        if (literalsOrFunctions.isEmpty()) return null;

        val isInLibrary = LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject()) != null
        val typeMapper = if (!isInLibrary)
            prepareTypeMapper(file)
        else
            createTypeMapperForLibraryFile(file.findElementAt(start), file)

        val currentLocationClassName = JvmClassName.byFqNameWithoutInnerClasses(FqName(currentLocationFqName)).getInternalName()
        for (literal in literalsOrFunctions) {
            val functionLiteral = literal as JetFunction
            if (isInlinedLambda(functionLiteral, typeMapper.getBindingContext())) {
                continue
            }

            val internalClassName = getInternalClassNameForElement(literal.getFirstChild(), typeMapper, file, isInLibrary).className
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

        val project = myDebugProcess.getProject()

        if (DumbService.getInstance(project).isDumb()) return null

        return DebuggerUtils.findSourceFileForClass(project, GlobalSearchScope.allScope(project), className, sourceName, location.lineNumber() - 1)
    }

    private fun defaultInternalName(location: Location): String {
        //no stratum or source path => use default one
        val referenceFqName = location.declaringType().name()
        // JDI names are of form "package.Class$InnerClass"
        return referenceFqName.replace('.', '/')
    }

    override fun getAllClasses(sourcePosition: SourcePosition): List<ReferenceType> {
        val psiFile = sourcePosition.getFile()
        if (psiFile is JetFile) {
            val names = classNameForPositionAndInlinedOnes(sourcePosition)
            val result = ArrayList<ReferenceType>()
            for (name in names) {
                result.addAll(myDebugProcess.getVirtualMachineProxy().classesByName(name))
            }
            return result
        }

        if (psiFile is ClsFileImpl) {
            val decompiledPsiFile = psiFile.getDecompiledPsiFile()
            if (decompiledPsiFile is JetClsFile && sourcePosition.getLine() == -1) {
                val className = PackageClassUtils.getPackageClassInternalName(decompiledPsiFile.getPackageFqName())
                return myDebugProcess.getVirtualMachineProxy().classesByName(className)
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
        val list = findInlinedCalls(sourcePosition.getElementAt(), sourcePosition.getFile())
        result.addAll(list)

        return result;
    }

    public fun classNameForPosition(sourcePosition: SourcePosition): String? {
        val psiElement = sourcePosition.getElementAt()
        if (psiElement == null) {
            return null
        }
        return classNameForPosition(psiElement)
    }

    private fun classNameForPosition(element: PsiElement): String? {
        return runReadAction {
            val file = element.getContainingFile() as JetFile
            val isInLibrary = LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject()) != null
            val typeMapper = if (!isInLibrary) prepareTypeMapper(file) else createTypeMapperForLibraryFile(element, file)
            getInternalClassNameForElement(element, typeMapper, file, isInLibrary).className
        }
    }

    private fun prepareTypeMapper(file: JetFile): JetTypeMapper {
        val key = createKeyForTypeMapper(file)

        var value: CachedValue<JetTypeMapper>? = myTypeMappers.get(key)
        if (value == null) {
            value = CachedValuesManager.getManager(file.getProject()).createCachedValue<JetTypeMapper>(
                    {
                        val typeMapper = createTypeMapper(file)
                        CachedValueProvider.Result<JetTypeMapper>(typeMapper, PsiModificationTracker.MODIFICATION_COUNT)
                    }, false)

            myTypeMappers.put(key, value)
        }

        return value!!.getValue()
    }

    override fun locationsOfLine(type: ReferenceType, position: SourcePosition): List<Location> {
        if (position.getFile() !is JetFile) {
            throw NoDataException.INSTANCE
        }
        try {
            val line = position.getLine() + 1
            val locations = if (myDebugProcess.getVirtualMachineProxy().versionHigher("1.4"))
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

    [deprecated("Since Idea 14.0.3 use createPrepareRequests fun")]
    override fun createPrepareRequest(classPrepareRequestor: ClassPrepareRequestor, sourcePosition: SourcePosition): ClassPrepareRequest? {
        if (sourcePosition.getFile() !is JetFile) {
            throw NoDataException.INSTANCE
        }
        val className = classNameForPosition(sourcePosition)
        if (className == null) {
            return null
        }
        return myDebugProcess.getRequestsManager().createClassPrepareRequest(classPrepareRequestor, className.replace('/', '.'))
    }

    override fun createPrepareRequests(requestor: ClassPrepareRequestor, position: SourcePosition): List<ClassPrepareRequest> {
        if (position.getFile() !is JetFile) {
            throw NoDataException.INSTANCE
        }

        return classNameForPositionAndInlinedOnes(position).map {
            className -> myDebugProcess.getRequestsManager().createClassPrepareRequest(requestor, className.replace('/', '.'))
        }
    }

    TestOnly
    public fun addTypeMapper(file: JetFile, typeMapper: JetTypeMapper) {
        val value = CachedValuesManager.getManager(file.getProject()).createCachedValue<JetTypeMapper>(
                { CachedValueProvider.Result<JetTypeMapper>(typeMapper, PsiModificationTracker.MODIFICATION_COUNT) }, false)
        val key = createKeyForTypeMapper(file)
        myTypeMappers.put(key, value)
    }

    companion object {
        public fun createTypeMapper(file: JetFile): JetTypeMapper {
            val project = file.getProject()

            val analysisResult = file.analyzeFullyAndGetResult()
            analysisResult.throwIfError()

            val state = GenerationState(project, ClassBuilderFactories.THROW_EXCEPTION, analysisResult.moduleDescriptor,
                                        analysisResult.bindingContext, listOf(file))
            state.beforeCompile()
            return state.getTypeMapper()
        }

        public fun getInternalClassNameForElement(notPositionedElement: PsiElement?, typeMapper: JetTypeMapper, file: JetFile, isInLibrary: Boolean): PositionedElement {
            val element = getElementToCalculateClassName(notPositionedElement)
            when {
                element is JetClassOrObject -> return PositionedElement(getJvmInternalNameForImpl(typeMapper, element), element)
                element is JetFunctionLiteral -> {
                    if (isInlinedLambda(element, typeMapper.getBindingContext())) {
                        return getInternalClassNameForElement(element.getParent(), typeMapper, file, isInLibrary)
                    }
                    else {
                        val asmType = CodegenBinding.asmTypeForAnonymousClass(typeMapper.getBindingContext(), element)
                        return PositionedElement(asmType.getInternalName(), element)
                    }
                }
                element is JetClassInitializer -> {
                    val parent = getElementToCalculateClassName(element.getParent())
                    // Class-object initializer
                    if (parent is JetObjectDeclaration && parent.isCompanion()) {
                        return PositionedElement(getInternalClassNameForElement(parent.getParent(), typeMapper, file, isInLibrary).className, parent)
                    }
                    return getInternalClassNameForElement(element, typeMapper, file, isInLibrary)
                }
                element is JetProperty && (!element.isTopLevel() || !isInLibrary) -> {
                    if (isInPropertyAccessor(notPositionedElement)) {
                        val classOrObject = PsiTreeUtil.getParentOfType(element, javaClass<JetClassOrObject>())
                        if (classOrObject != null) {
                            return PositionedElement(getJvmInternalNameForImpl(typeMapper, classOrObject), element)
                        }
                    }

                    val descriptor = typeMapper.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
                    if (descriptor !is PropertyDescriptor) {
                        return getInternalClassNameForElement(element.getParent(), typeMapper, file, isInLibrary)
                    }

                    return PositionedElement(getJvmInternalNameForPropertyOwner(typeMapper, descriptor), element)
                }
                element is JetNamedFunction -> {
                    if (isInlinedLambda(element, typeMapper.getBindingContext())) {
                        return getInternalClassNameForElement(element.getParent(), typeMapper, file, isInLibrary)
                    }

                    val parent = getElementToCalculateClassName(element)
                    if (parent is JetClassOrObject) {
                        return PositionedElement(getJvmInternalNameForImpl(typeMapper, parent), element)
                    }
                    else if (parent != null) {
                        val asmType = CodegenBinding.asmTypeForAnonymousClass(typeMapper.getBindingContext(), element)
                        return PositionedElement(asmType.getInternalName(), element)
                    }
                }
            }

            if (isInLibrary) {
                val elementAtForLibraryFile = getElementToCreateTypeMapperForLibraryFile(notPositionedElement)
                assert(elementAtForLibraryFile != null) {
                    "Couldn't find element at breakpoint for library file " + file.getName() +
                         (if (notPositionedElement == null) "" else ", notPositionedElement = " + notPositionedElement.getElementTextWithContext())
                }
                return PositionedElement(findPackagePartInternalNameForLibraryFile(elementAtForLibraryFile!!), elementAtForLibraryFile)
            }

            return PositionedElement(PackagePartClassUtils.getPackagePartInternalName(file), element)
        }

        private fun getElementToCalculateClassName(notPositionedElement: PsiElement?): JetElement? =
            PsiTreeUtil.getParentOfType(notPositionedElement,
                                        javaClass<JetClassOrObject>(),
                                        javaClass<JetFunctionLiteral>(),
                                        javaClass<JetNamedFunction>(),
                                        javaClass<JetProperty>(),
                                        javaClass<JetClassInitializer>())

        public fun getJvmInternalNameForPropertyOwner(typeMapper: JetTypeMapper, descriptor: PropertyDescriptor): String {
            return typeMapper.mapOwner(
                    if (AsmUtil.isPropertyWithBackingFieldInOuterClass(descriptor)) descriptor.getContainingDeclaration() else descriptor,
                    true).getInternalName()
        }

        private fun isInPropertyAccessor(element: PsiElement?) =
                element is JetPropertyAccessor ||
                PsiTreeUtil.getParentOfType(element, javaClass<JetProperty>(), javaClass<JetPropertyAccessor>()) is JetPropertyAccessor

        private fun getElementToCreateTypeMapperForLibraryFile(element: PsiElement?) =
                if (element is JetElement) element else PsiTreeUtil.getParentOfType(element, javaClass<JetElement>())

        private fun getJvmInternalNameForImpl(typeMapper: JetTypeMapper, jetClass: JetClassOrObject): String? {
            val classDescriptor = typeMapper.getBindingContext().get<PsiElement, ClassDescriptor>(BindingContext.CLASS, jetClass)
            if (classDescriptor == null) {
                return null
            }

            if (jetClass is JetClass && jetClass.isInterface()) {
                return typeMapper.mapTraitImpl(classDescriptor).getInternalName()
            }

            return typeMapper.mapClass(classDescriptor).getInternalName()
        }

        private fun createTypeMapperForLibraryFile(notPositionedElement: PsiElement?, file: JetFile): JetTypeMapper {
            val element = getElementToCreateTypeMapperForLibraryFile(notPositionedElement)
            val analysisResult = element!!.analyzeAndGetResult()

            val state = GenerationState(file.getProject(), ClassBuilderFactories.THROW_EXCEPTION,
                                        analysisResult.moduleDescriptor, analysisResult.bindingContext, listOf(file))
            state.beforeCompile()
            return state.getTypeMapper()
        }

        public fun isInlinedLambda(functionLiteral: JetFunction, context: BindingContext): Boolean {
            return InlineUtil.isInlinedArgument(functionLiteral, context, false)
        }

        private fun createKeyForTypeMapper(file: JetFile) = PackagePartClassUtils.getPackagePartInternalName(file)
    }

    private fun findInlinedCalls(element: PsiElement?, jetFile: PsiFile?): List<String> {
        if (element == null || jetFile !is JetFile) {
            return emptyList()
        }

        return runReadAction {
            val result = arrayListOf<String>()
            val isInLibrary = LibraryUtil.findLibraryEntry(jetFile.getVirtualFile(), jetFile.getProject()) != null
            val typeMapper = if (!isInLibrary) prepareTypeMapper(jetFile) else createTypeMapperForLibraryFile(element, jetFile)
            val psiElement = getInternalClassNameForElement(element, typeMapper, jetFile, isInLibrary).element;

            if (psiElement is JetNamedFunction &&
                InlineUtil.isInline(typeMapper.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiElement))
            ) {
                val project = myDebugProcess.getProject()
                val usagesSearchTarget = FindUsagesOptions(project).toSearchTarget(psiElement, true)

                val usagesSearchRequest = DefaultSearchHelper<JetNamedFunction>(true).newRequest(usagesSearchTarget)
                usagesSearchRequest.search().forEach {
                    val psiElement = it.getElement()
                    if (psiElement is JetElement) {
                        //TODO recursive search
                        val name = classNameForPosition(psiElement)
                        if (name != null) {
                            result.add(name)
                        }
                    }
                }
            }
            result
        }
    }
}
