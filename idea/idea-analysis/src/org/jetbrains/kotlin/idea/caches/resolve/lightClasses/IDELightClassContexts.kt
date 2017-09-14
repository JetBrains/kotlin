/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve.lightClasses

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.idea.caches.resolve.getModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.IDELightClassConstructionContext.Mode.EXACT
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.IDELightClassConstructionContext.Mode.LIGHT
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.idea.project.IdeaEnvironment
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.idea.stubindex.KotlinOverridableInternalMembersShortNameIndex
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.WrappedTypeFactory
import org.jetbrains.kotlin.utils.sure


class IDELightClassConstructionContext(bindingContext: BindingContext, module: ModuleDescriptor, val mode: Mode)
    : LightClassConstructionContext(bindingContext, module) {
    enum class Mode {
        LIGHT,
        EXACT
    }

    override fun toString() = "${this.javaClass.simpleName}:$mode"
}

object IDELightClassContexts {

    private val LOG = Logger.getInstance(this::class.java)

    fun contextForNonLocalClassOrObject(classOrObject: KtClassOrObject): LightClassConstructionContext {
        val resolutionFacade = classOrObject.getResolutionFacade()
        val bindingContext = if (classOrObject is KtClass && classOrObject.isAnnotation()) {
            // need to make sure default values for parameters are resolved
            // because java resolve depends on whether there is a default value for an annotation attribute
            resolutionFacade.getFrontendService(ResolveElementCache::class.java)
                    .resolvePrimaryConstructorParametersDefaultValues(classOrObject)
        }
        else {
            resolutionFacade.analyze(classOrObject)
        }
        val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject).sure {
            "Class descriptor was not found for ${classOrObject.getElementTextWithContext()}"
        }
        ForceResolveUtil.forceResolveAllContents(classDescriptor)
        return IDELightClassConstructionContext(bindingContext, resolutionFacade.moduleDescriptor, EXACT)
    }

    fun contextForLocalClassOrObject(classOrObject: KtClassOrObject): LightClassConstructionContext {
        val resolutionFacade = classOrObject.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(classOrObject)

        val descriptor = bindingContext.get(BindingContext.CLASS, classOrObject)

        if (descriptor == null) {
            LOG.warn("No class descriptor in context for class: " + classOrObject.getElementTextWithContext())
            return IDELightClassConstructionContext(bindingContext, resolutionFacade.moduleDescriptor, EXACT)
        }

        ForceResolveUtil.forceResolveAllContents(descriptor)

        return IDELightClassConstructionContext(bindingContext, resolutionFacade.moduleDescriptor, EXACT)
    }


    fun contextForFacade(files: List<KtFile>): LightClassConstructionContext {
        val resolveSession = files.first().getResolutionFacade().getFrontendService(ResolveSession::class.java)

        forceResolvePackageDeclarations(files, resolveSession)
        return IDELightClassConstructionContext(resolveSession.bindingContext, resolveSession.moduleDescriptor, EXACT)
    }

    fun contextForScript(script: KtScript): LightClassConstructionContext {
        val resolutionFacade = script.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(script)

        val descriptor = bindingContext[BindingContext.SCRIPT, script]
        if (descriptor == null) {
            LOG.warn("No script descriptor in context for script: " + script.getElementTextWithContext())
            return IDELightClassConstructionContext(bindingContext, resolutionFacade.moduleDescriptor, EXACT)
        }

        ForceResolveUtil.forceResolveAllContents(descriptor)

        return IDELightClassConstructionContext(bindingContext, resolutionFacade.moduleDescriptor, EXACT)
    }

    fun lightContextForClassOrObject(classOrObject: KtClassOrObject): LightClassConstructionContext? {
        if (!isDummyResolveApplicable(classOrObject)) return null

        val resolveSession = setupAdHocResolve(classOrObject.project, classOrObject.getResolutionFacade().moduleDescriptor, listOf(classOrObject.containingKtFile))

        ForceResolveUtil.forceResolveAllContents(resolveSession.resolveToDescriptor(classOrObject))

        return IDELightClassConstructionContext(resolveSession.bindingContext, resolveSession.moduleDescriptor, LIGHT)
    }

    fun lightContextForFacade(files: List<KtFile>): LightClassConstructionContext {
        val representativeFile = files.first()
        val resolveSession = setupAdHocResolve(representativeFile.project, representativeFile.getResolutionFacade().moduleDescriptor, files)

        forceResolvePackageDeclarations(files, resolveSession)

        return IDELightClassConstructionContext(resolveSession.bindingContext, resolveSession.moduleDescriptor, LIGHT)
    }

    fun lightContextForScript(script: KtScript): LightClassConstructionContext {
        val resolveSession = setupAdHocResolve(
                script.project,
                script.getResolutionFacade().moduleDescriptor,
                listOf(script.containingKtFile))

        ForceResolveUtil.forceResolveAllContents(resolveSession.resolveToDescriptor(script))

        return IDELightClassConstructionContext(resolveSession.bindingContext, resolveSession.moduleDescriptor, LIGHT)
    }

    private fun isDummyResolveApplicable(classOrObject: KtClassOrObject): Boolean {
        if (classOrObject.hasLightClassMatchingErrors) return false

        if (hasDelegatedSupertypes(classOrObject)) return false

        if (isDataClassWithGeneratedMembersOverridden(classOrObject)) return false

        if (hasMembersOverridingInternalMembers(classOrObject)) return false

        return classOrObject.declarations.filterIsInstance<KtClassOrObject>().all { isDummyResolveApplicable(it) }
    }

    private fun hasDelegatedSupertypes(classOrObject: KtClassOrObject) = classOrObject.superTypeListEntries.any { it is KtDelegatedSuperTypeEntry }

    private fun isDataClassWithGeneratedMembersOverridden(classOrObject: KtClassOrObject): Boolean {
        return classOrObject.hasModifier(KtTokens.DATA_KEYWORD) &&
               classOrObject.declarations.filterIsInstance<KtFunction>().any {
                   isGeneratedForDataClass(it.nameAsSafeName)
               }
    }

    private fun isGeneratedForDataClass(name: Name): Boolean {
        return name == DataClassDescriptorResolver.EQUALS_METHOD_NAME ||
               // known failure is related to equals override, checking for other methods 'just in case'
               name == DataClassDescriptorResolver.COPY_METHOD_NAME ||
               name == DataClassDescriptorResolver.HASH_CODE_METHOD_NAME ||
               name == DataClassDescriptorResolver.TO_STRING_METHOD_NAME ||
               DataClassDescriptorResolver.isComponentLike(name)
    }

    private fun hasMembersOverridingInternalMembers(classOrObject: KtClassOrObject): Boolean {
        return classOrObject.declarations.filterIsInstance<KtCallableDeclaration>().any {
            possiblyOverridesInternalMember(it)
        }
    }

    private fun possiblyOverridesInternalMember(declaration: KtCallableDeclaration): Boolean {
        if (!declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return false

        return declaration.name?.let { anyInternalMembersWithThisName(it, declaration.project) } ?: false
    }

    private fun anyInternalMembersWithThisName(name: String, project: Project): Boolean {
        var result = false
        StubIndex.getInstance().processElements(
                KotlinOverridableInternalMembersShortNameIndex.Instance.key, name, project,
                EverythingGlobalScope(project), KtCallableDeclaration::class.java
        ) {
            result = true
            false // stop processing at first matching result
        }
        return result
    }

    fun forceResolvePackageDeclarations(files: Collection<KtFile>, session: ResolveSession) {
        for (file in files) {
            if (file.isScript()) continue

            val packageFqName = file.packageFqName

            // make sure we create a package descriptor
            val packageDescriptor = session.moduleDescriptor.getPackage(packageFqName)
            if (packageDescriptor.isEmpty()) {
                LOG.warn("No descriptor found for package " + packageFqName + " in file " + file.name + "\n" + file.text)
                session.forceResolveAll()
                continue
            }

            for (declaration in file.declarations) {
                when (declaration) {
                    is KtFunction -> {
                        val name = declaration.nameAsSafeName
                        val functions = packageDescriptor.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)
                        for (descriptor in functions) {
                            ForceResolveUtil.forceResolveAllContents(descriptor)
                        }
                    }
                    is KtProperty -> {
                        val name = declaration.nameAsSafeName
                        val properties = packageDescriptor.memberScope.getContributedVariables(name, NoLookupLocation.FROM_IDE)
                        for (descriptor in properties) {
                            ForceResolveUtil.forceResolveAllContents(descriptor)
                        }
                    }
                    is KtClassOrObject, is KtTypeAlias, is KtDestructuringDeclaration -> {
                        // Do nothing: we are not interested in classes or type aliases,
                        // and all destructuring declarations are erroneous at top level
                    }
                    else -> LOG.error("Unsupported declaration kind: " + declaration + " in file " + file.name + "\n" + file.text)
                }
            }

            ForceResolveUtil.forceResolveAllContents(session.getFileAnnotations(file))
        }
    }


    private fun setupAdHocResolve(project: Project, realWorldModule: ModuleDescriptor, files: List<KtFile>): ResolveSession {
        val trace = BindingTraceContext()
        val sm = LockBasedStorageManager.NO_LOCKS
        val moduleDescriptor = ModuleDescriptorImpl(realWorldModule.name, sm, realWorldModule.builtIns)

        setupDependencies(moduleDescriptor, realWorldModule)

        val moduleInfo = files.first().getModuleInfo()
        val container = createContainer("LightClassStub", JvmPlatform) {
            val jvmTarget = IDELanguageSettingsProvider.getTargetPlatform(moduleInfo) as? JvmTarget
            configureModule(
                    ModuleContext(moduleDescriptor, project), JvmPlatform,
                    jvmTarget ?: JvmTarget.DEFAULT, trace
            )

            useInstance(GlobalSearchScope.EMPTY_SCOPE)
            useInstance(LookupTracker.DO_NOTHING)
            useImpl<FileScopeProviderImpl>()
            useInstance(IDELanguageSettingsProvider.getLanguageVersionSettings(moduleInfo, project))
            useInstance(FileBasedDeclarationProviderFactory(sm, files))

            useImpl<AdHocAnnotationResolver>()

            useInstance(object : WrappedTypeFactory(sm) {
                override fun createDeferredType(trace: BindingTrace, computation: () -> KotlinType) = errorType()

                override fun createRecursionIntolerantDeferredType(trace: BindingTrace, computation: () -> KotlinType) = errorType()

                private fun errorType() = ErrorUtils.createErrorType("Error type in ad hoc resolve for lighter classes")
            })

            IdeaEnvironment.configure(this)

            useImpl<ResolveSession>()
        }


        val resolveSession = container.get<ResolveSession>()
        moduleDescriptor.initialize(CompositePackageFragmentProvider(listOf(resolveSession.packageFragmentProvider)))
        return resolveSession
    }

    private fun setupDependencies(moduleDescriptor: ModuleDescriptorImpl, realWorldModule: ModuleDescriptor) {
        val jvmFieldClass = realWorldModule.getPackage(FqName("kotlin.jvm")).memberScope
                .getContributedClassifier(Name.identifier("JvmField"), NoLookupLocation.FROM_IDE)

        if (jvmFieldClass != null) {
            moduleDescriptor.setDependencies(moduleDescriptor, jvmFieldClass.module as ModuleDescriptorImpl, moduleDescriptor.builtIns.builtInsModule)
        }
        else {
            moduleDescriptor.setDependencies(moduleDescriptor, moduleDescriptor.builtIns.builtInsModule)
        }
    }

    // see JvmPlatformAnnotations.kt, JvmFlagAnnotations.kt, also PsiModifier.MODIFIERS
    private val annotationsThatAffectCodegen = listOf(
            "JvmField", "JvmOverloads", "JvmName", "JvmStatic",
            "Synchronized", "Transient", "Volatile", "Strictfp"
    ).map { FqName("kotlin.jvm").child(Name.identifier(it)) } + FqName("kotlin.PublishedApi") + FqName("kotlin.Deprecated")

    class AdHocAnnotationResolver(
            private val moduleDescriptor: ModuleDescriptor,
            private val callResolver: CallResolver,
            constantExpressionEvaluator: ConstantExpressionEvaluator,
            storageManager: StorageManager
    ) : AnnotationResolverImpl(callResolver, constantExpressionEvaluator, storageManager) {

        override fun resolveAnnotationType(scope: LexicalScope, entryElement: KtAnnotationEntry, trace: BindingTrace): KotlinType {
            return annotationClassByEntry(entryElement)?.defaultType ?: super.resolveAnnotationType(scope, entryElement, trace)
        }

        private fun annotationClassByEntry(entryElement: KtAnnotationEntry): ClassDescriptor? {
            val annotationTypeReferencePsi = entryElement.calleeExpression?.constructorReferenceExpression ?: return null
            val referencedName = annotationTypeReferencePsi.getReferencedName()
            for (annotationFqName in annotationsThatAffectCodegen) {
                if (referencedName == annotationFqName.shortName().asString()) {
                    moduleDescriptor.getPackage(annotationFqName.parent()).memberScope
                            .getContributedClassifier(annotationFqName.shortName(), NoLookupLocation.FROM_IDE)?.let { return it as? ClassDescriptor }

               }
            }
            return null
        }

        override fun resolveAnnotationCall(annotationEntry: KtAnnotationEntry, scope: LexicalScope, trace: BindingTrace): OverloadResolutionResults<FunctionDescriptor> {
            val annotationConstructor = annotationClassByEntry(annotationEntry)?.constructors?.singleOrNull()
                                        ?: return super.resolveAnnotationCall(annotationEntry, scope, trace)

            @Suppress("UNCHECKED_CAST")
            return callResolver.resolveConstructorCall(
                    BasicCallResolutionContext.create(
                            trace, scope, CallMaker.makeCall(null, null, annotationEntry), TypeUtils.NO_EXPECTED_TYPE,
                            DataFlowInfoFactory.EMPTY, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                            true
                    ),
                    annotationEntry.calleeExpression!!.constructorReferenceExpression!!,
                    annotationConstructor.returnType
            ) as OverloadResolutionResults<FunctionDescriptor>
        }
    }
}

