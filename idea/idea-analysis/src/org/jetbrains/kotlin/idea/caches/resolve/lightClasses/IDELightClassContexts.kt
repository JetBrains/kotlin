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
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.LanguageSettingsProvider
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.LazyResolveToken
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.idea.caches.resolve.getModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.IDELightClassConstructionContext.Mode.EXACT
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.IDELightClassConstructionContext.Mode.LIGHT
import org.jetbrains.kotlin.idea.project.IdeaEnvironment
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.StringValue
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
import org.jetbrains.kotlin.types.WrappedTypeFactory
import org.jetbrains.kotlin.utils.sure


class IDELightClassConstructionContext(bindingContext: BindingContext, module: ModuleDescriptor, val mode: Mode)
    : LightClassConstructionContext(bindingContext, module) {
    enum class Mode {
        LIGHT,
        EXACT
    }
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

    fun lightContextForClassOrObject(classOrObject: KtClassOrObject): LightClassConstructionContext {
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

    fun forceResolvePackageDeclarations(files: Collection<KtFile>, session: ResolveSession) {
        for (file in files) {
            if (file.isScript) continue

            val packageFqName = file.packageFqName

            // make sure we create a package descriptor
            val packageDescriptor = session.moduleDescriptor.getPackage(packageFqName)
            if (packageDescriptor.isEmpty()) {
                LOG.warn("No descriptor found for package " + packageFqName + " in file " + file.name + "\n" + file.text)
                session.forceResolveAll()
                continue
            }

            for (declaration in file.declarations) {
                if (declaration is KtFunction) {
                    val name = declaration.nameAsSafeName
                    val functions = packageDescriptor.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)
                    for (descriptor in functions) {
                        ForceResolveUtil.forceResolveAllContents(descriptor)
                    }
                }
                else if (declaration is KtProperty) {
                    val name = declaration.nameAsSafeName
                    val properties = packageDescriptor.memberScope.getContributedVariables(name, NoLookupLocation.FROM_IDE)
                    for (descriptor in properties) {
                        ForceResolveUtil.forceResolveAllContents(descriptor)
                    }
                }
                else if (declaration is KtClassOrObject || declaration is KtTypeAlias || declaration is KtDestructuringDeclaration) {
                    // Do nothing: we are not interested in classes or type aliases,
                    // and all destructuring declarations are erroneous at top level
                }
                else {
                    LOG.error("Unsupported declaration kind: " + declaration + " in file " + file.name + "\n" + file.text)
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
            val jvmTarget = LanguageSettingsProvider.getInstance(project).getTargetPlatform(moduleInfo) as? JvmTarget
            configureModule(
                    ModuleContext(moduleDescriptor, project), JvmPlatform,
                    jvmTarget ?: JvmTarget.DEFAULT, trace
            )

            useInstance(GlobalSearchScope.EMPTY_SCOPE)
            useInstance(LookupTracker.DO_NOTHING)
            useImpl<FileScopeProviderImpl>()
            useInstance(LanguageSettingsProvider.getInstance(project).getLanguageVersionSettings(moduleInfo, project))
            useInstance(FileBasedDeclarationProviderFactory(sm, files))

            useImpl<AdHocAnnotationResolver>()

            useInstance(object : WrappedTypeFactory(sm) {
                override fun createLazyWrappedType(computation: () -> KotlinType): KotlinType = errorType()

                override fun createDeferredType(trace: BindingTrace, computation: () -> KotlinType) = errorType()

                override fun createRecursionIntolerantDeferredType(trace: BindingTrace, computation: () -> KotlinType) = errorType()

                private fun errorType() = ErrorUtils.createErrorType("Error type in ad hoc resolve for lighter classes")
            })

            IdeaEnvironment.configure(this)
            useImpl<LazyResolveToken>()

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
    ).map { FqName("kotlin.jvm").child(Name.identifier(it)) } + FqName("kotlin.PublishedApi")

    class AdHocAnnotationResolver(
            private val moduleDescriptor: ModuleDescriptor,
            callResolver: CallResolver,
            constantExpressionEvaluator: ConstantExpressionEvaluator,
            storageManager: StorageManager
    ) : AnnotationResolverImpl(callResolver, constantExpressionEvaluator, storageManager) {

        override fun resolveAnnotationEntries(scope: LexicalScope, annotationEntries: List<KtAnnotationEntry>, trace: BindingTrace, shouldResolveArguments: Boolean): Annotations {
            return super.resolveAnnotationEntries(scope, annotationEntries, trace, shouldResolveArguments)
        }

        override fun resolveAnnotationType(scope: LexicalScope, entryElement: KtAnnotationEntry, trace: BindingTrace): KotlinType {
            return annotationClassByEntry(entryElement)?.defaultType ?: super.resolveAnnotationType(scope, entryElement, trace)
        }

        private fun annotationClassByEntry(entryElement: KtAnnotationEntry): ClassDescriptor? {
            val annotationTypeReferencePsi = (entryElement.typeReference?.typeElement as? KtUserType)?.referenceExpression ?: return null
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
            val valueArgumentText = valueArgumentText(annotationEntry)
                                    ?: return super.resolveAnnotationCall(annotationEntry, scope, trace)
            val fakeResolvedCall = object : ResolvedCall<FunctionDescriptor> {
                override fun getStatus() = ResolutionStatus.SUCCESS
                override fun getCandidateDescriptor() = annotationConstructor
                override fun getResultingDescriptor() = annotationConstructor
                override fun getValueArguments() =
                        annotationConstructor.valueParameters.singleOrNull()?.let { mapOf(it to FakeResolvedValueArgument(valueArgumentText)) }.orEmpty()

                override fun getCall() = notImplemented
                override fun getExtensionReceiver() = notImplemented
                override fun getDispatchReceiver() = notImplemented
                override fun getExplicitReceiverKind() = notImplemented

                override fun getValueArgumentsByIndex() = notImplemented
                override fun getArgumentMapping(valueArgument: ValueArgument) = notImplemented
                override fun getTypeArguments() = notImplemented
                override fun getDataFlowInfoForArguments() = notImplemented
                override fun getSmartCastDispatchReceiverType() = notImplemented
            }

            return object : OverloadResolutionResults<FunctionDescriptor> {
                override fun isSingleResult() = true
                override fun getResultingCall(): ResolvedCall<FunctionDescriptor> = fakeResolvedCall
                override fun getResultingDescriptor() = annotationConstructor
                override fun getAllCandidates() = notImplemented
                override fun getResultingCalls() = notImplemented
                override fun getResultCode() = notImplemented
                override fun isSuccess() = notImplemented
                override fun isNothing() = notImplemented
                override fun isAmbiguity() = notImplemented
                override fun isIncomplete() = notImplemented
            }
        }

        private fun valueArgumentText(annotationEntry: KtAnnotationEntry) =
                ((annotationEntry.valueArguments.singleOrNull()?.getArgumentExpression() as? KtStringTemplateExpression)?.entries?.singleOrNull() as? KtLiteralStringTemplateEntry)?.text

        override fun getAnnotationArgumentValue(trace: BindingTrace, valueParameter: ValueParameterDescriptor, resolvedArgument: ResolvedValueArgument): ConstantValue<*>? {
            if (resolvedArgument is FakeResolvedValueArgument) return StringValue(resolvedArgument.argumentText, moduleDescriptor.builtIns)

            return super.getAnnotationArgumentValue(trace, valueParameter, resolvedArgument)
        }

        private class FakeResolvedValueArgument(val argumentText: String) : ResolvedValueArgument {
            override fun getArguments() = notImplemented
        }
    }

    private val notImplemented: Nothing
            get() = error("Should not be called")
}

