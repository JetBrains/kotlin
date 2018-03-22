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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.psi.impl.CompositeShortNamesCache
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.util.indexing.IdFilter
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.KotlinShortNamesCache
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaFieldDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.extension.KotlinIndicesHelperExtension
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.search.excludeKotlinSources
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.receiverTypes
import org.jetbrains.kotlin.idea.util.substituteExtensionIfCallable
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DeprecationResolver
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.collectSyntheticStaticFunctions
import org.jetbrains.kotlin.types.KotlinType
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

class KotlinIndicesHelper(
        private val resolutionFacade: ResolutionFacade,
        private val scope: GlobalSearchScope,
        visibilityFilter: (DeclarationDescriptor) -> Boolean,
        private val declarationTranslator: (KtDeclaration) -> KtDeclaration? = { it },
        applyExcludeSettings: Boolean = true,
        private val filterOutPrivate: Boolean = true,
        private val file: KtFile? = null
) {

    private val moduleDescriptor = resolutionFacade.moduleDescriptor
    private val project = resolutionFacade.project
    private val scopeWithoutKotlin = scope.excludeKotlinSources() as GlobalSearchScope

    private val descriptorFilter: (DeclarationDescriptor) -> Boolean = filter@ {
        if (resolutionFacade.frontendService<DeprecationResolver>().isHiddenInResolution(it)) return@filter false
        if (!visibilityFilter(it)) return@filter false
        if (applyExcludeSettings && it.isExcludedFromAutoImport(project, file)) return@filter false
        true
    }

    fun getTopLevelCallablesByName(name: String): Collection<CallableDescriptor> {
        val declarations = LinkedHashSet<KtNamedDeclaration>()
        declarations.addTopLevelNonExtensionCallablesByName(KotlinFunctionShortNameIndex.getInstance(), name)
        declarations.addTopLevelNonExtensionCallablesByName(KotlinPropertyShortNameIndex.getInstance(), name)
        return declarations
                .flatMap { it.resolveToDescriptors<CallableDescriptor>() }
                .filter { descriptorFilter(it) }
    }

    private fun MutableSet<KtNamedDeclaration>.addTopLevelNonExtensionCallablesByName(
            index: StringStubIndexExtension<out KtNamedDeclaration>,
            name: String
    ) {
        index.get(name, project, scope).filterTo(this) { it.parent is KtFile && it is KtCallableDeclaration && it.receiverTypeReference == null }
    }

    fun getTopLevelExtensionOperatorsByName(name: String): Collection<FunctionDescriptor> {
        return KotlinFunctionShortNameIndex.getInstance().get(name, project, scope)
                .filter { it.parent is KtFile && it.receiverTypeReference != null && it.hasModifier(KtTokens.OPERATOR_KEYWORD) }
                .flatMap { it.resolveToDescriptors<FunctionDescriptor>() }
                .filter { descriptorFilter(it) && it.extensionReceiverParameter != null }
                .distinct()
    }

    fun getMemberOperatorsByName(name: String): Collection<FunctionDescriptor> {
        return KotlinFunctionShortNameIndex.getInstance().get(name, project, scope)
                .filter { it.parent is KtClassBody && it.receiverTypeReference == null && it.hasModifier(KtTokens.OPERATOR_KEYWORD) }
                .flatMap { it.resolveToDescriptors<FunctionDescriptor>() }
                .filter { descriptorFilter(it) && it.extensionReceiverParameter == null }
                .distinct()
    }

    fun processTopLevelCallables(nameFilter: (String) -> Boolean, processor: (CallableDescriptor) -> Unit) {
        fun processIndex(index: StringStubIndexExtension<out KtCallableDeclaration>) {
            for (key in index.getAllKeys(project)) {
                ProgressManager.checkCanceled()
                if (!nameFilter(key.substringAfterLast('.', key))) continue

                for (declaration in index.get(key, project, scope)) {
                    if (declaration.receiverTypeReference != null) continue
                    if (filterOutPrivate && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) continue

                    for (descriptor in declaration.resolveToDescriptors<CallableDescriptor>()) {
                        if (descriptorFilter(descriptor)) {
                            processor(descriptor)
                        }
                    }
                }
            }
        }
        processIndex(KotlinTopLevelFunctionFqnNameIndex.getInstance())
        processIndex(KotlinTopLevelPropertyFqnNameIndex.getInstance())
    }

    fun getCallableTopLevelExtensions(
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            position: KtExpression,
            bindingContext: BindingContext,
            nameFilter: (String) -> Boolean
    ): Collection<CallableDescriptor> {
        val receiverTypes = callTypeAndReceiver.receiverTypes(bindingContext, position, moduleDescriptor, resolutionFacade, stableSmartCastsOnly = false)
                            ?: return emptyList()
        return getCallableTopLevelExtensions(callTypeAndReceiver, receiverTypes, nameFilter)
    }

    fun getCallableTopLevelExtensions(
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            receiverTypes: Collection<KotlinType>,
            nameFilter: (String) -> Boolean,
            declarationFilter: (KtDeclaration) -> Boolean = { true }
    ): Collection<CallableDescriptor> {
        if (receiverTypes.isEmpty()) return emptyList()

        val receiverTypeNames = HashSet<String>()
        receiverTypes.forEach { receiverTypeNames.addTypeNames(it) }

        val index = KotlinTopLevelExtensionsByReceiverTypeIndex.INSTANCE

        val declarations = index.getAllKeys(project)
                .asSequence()
                .filter {
                    ProgressManager.checkCanceled()
                    KotlinTopLevelExtensionsByReceiverTypeIndex.receiverTypeNameFromKey(it) in receiverTypeNames
                    && nameFilter(KotlinTopLevelExtensionsByReceiverTypeIndex.callableNameFromKey(it))
                }
                .flatMap { index.get(it, project, scope).asSequence() }.filter(declarationFilter)

        val suitableExtensions = findSuitableExtensions(declarations, receiverTypes, callTypeAndReceiver.callType)

        val additionalDescriptors = ArrayList<CallableDescriptor>(0)

        for (extension in KotlinIndicesHelperExtension.getInstances(project)) {
            extension.appendExtensionCallables(additionalDescriptors, moduleDescriptor, receiverTypes, nameFilter)
        }

        return if (additionalDescriptors.isNotEmpty())
            suitableExtensions + additionalDescriptors
        else
            suitableExtensions
    }


    fun resolveTypeAliasesUsingIndex(type: KotlinType, originalTypeName: String): Set<TypeAliasDescriptor> {
        val typeConstructor = type.constructor

        val index = KotlinTypeAliasByExpansionShortNameIndex.INSTANCE
        val out = mutableSetOf<TypeAliasDescriptor>()

        fun searchRecursively(typeName: String) {
            ProgressManager.checkCanceled()
            index[typeName, project, scope].asSequence()
                    .map { it.resolveToDescriptorIfAny() as? TypeAliasDescriptor }
                    .filterNotNull()
                    .filter { it.expandedType.constructor == typeConstructor }
                    .filter { it !in out }
                    .onEach { out.add(it) }
                    .map { it.name.asString() }
                    .forEach(::searchRecursively)
        }

        searchRecursively(originalTypeName)
        return out
    }

    private fun MutableCollection<String>.addTypeNames(type: KotlinType) {
        val constructor = type.constructor
        constructor.declarationDescriptor?.name?.asString()?.let { typeName ->
            add(typeName)
            resolveTypeAliasesUsingIndex(type, typeName).mapTo(this, { it.name.asString() })
        }
        constructor.supertypes.forEach { addTypeNames(it) }
    }

    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     */
    private fun findSuitableExtensions(
            declarations: Sequence<KtCallableDeclaration>,
            receiverTypes: Collection<KotlinType>,
            callType: CallType<*>
    ): Collection<CallableDescriptor> {
        val result = LinkedHashSet<CallableDescriptor>()

        fun processDescriptor(descriptor: CallableDescriptor) {
            if (descriptor.extensionReceiverParameter != null && descriptorFilter(descriptor)) {
                result.addAll(descriptor.substituteExtensionIfCallable(receiverTypes, callType))
            }
        }

        declarations.forEach { it.resolveToDescriptors<CallableDescriptor>().forEach(::processDescriptor) }

        return result
    }

    fun getJvmClassesByName(name: String): Collection<ClassDescriptor> {
        return PsiShortNamesCache.getInstance(project).getClassesByName(name, scope)
                .filter { it in scope && it.containingFile != null }
                .mapNotNull { it.resolveToDescriptor(resolutionFacade) }
                .filter(descriptorFilter)
                .toSet()
    }

    fun getKotlinEnumsByName(name: String): Collection<DeclarationDescriptor> {
        return KotlinClassShortNameIndex.getInstance()[name, project, scope]
                .filter { it is KtEnumEntry && it in scope }
                .mapNotNull { it.unsafeResolveToDescriptor() }
                .filter(descriptorFilter)
                .toSet()
    }

    fun processJvmCallablesByName(
            name: String,
            filter: (PsiMember) -> Boolean,
            processor: (CallableDescriptor) -> Unit
    ) {
        val javaDeclarations = getJavaCallables(name, PsiShortNamesCache.getInstance(project))
        val processed = HashSet<CallableDescriptor>()
        for (javaDeclaration in javaDeclarations) {
            ProgressManager.checkCanceled()
            if (javaDeclaration is KtLightElement<*, *>) continue
            if (!filter(javaDeclaration as PsiMember)) continue
            val descriptor = javaDeclaration.getJavaMemberDescriptor(resolutionFacade) as? CallableDescriptor ?: continue
            if (!processed.add(descriptor)) continue
            if (!descriptorFilter(descriptor)) continue
            processor(descriptor)
        }
    }

    /*
     * This is a dirty work-around to filter out results from BrShortNamesCache.
     * BrShortNamesCache creates a synthetic class (LightBrClass), which traverses all annotated properties
     *     in a module inside "myFieldCache" (and in Kotlin light classes too, of course).
     * It triggers the light classes compilation in the UI thread inside our static field import quick-fix.
     */
    private val filteredShortNamesCaches: List<PsiShortNamesCache>? by lazy {
        val shortNamesCache = PsiShortNamesCache.getInstance(project)
        if (shortNamesCache is CompositeShortNamesCache) {
            try {
                fun getMyCachesField(clazz: Class<out PsiShortNamesCache>): Field {
                    try {
                        return clazz.getDeclaredField("myCaches")
                    }
                    catch (e: NoSuchFieldException) {
                        // In case the class is proguarded
                        return clazz.declaredFields.first {
                            Modifier.isPrivate(it.modifiers) && Modifier.isFinal(it.modifiers) && !Modifier.isStatic(it.modifiers)
                            && it.type.isArray && it.type.componentType == PsiShortNamesCache::class.java
                        }
                    }
                }

                val myCachesField = getMyCachesField(shortNamesCache::class.java)
                val previousIsAccessible = myCachesField.isAccessible
                try {
                    myCachesField.isAccessible = true
                    @Suppress("UNCHECKED_CAST")
                    return@lazy (myCachesField.get(shortNamesCache) as Array<PsiShortNamesCache>).filter {
                        it !is KotlinShortNamesCache
                        && it::class.java.name != "com.android.tools.idea.databinding.BrShortNamesCache"
                        && it::class.java.name != "com.android.tools.idea.databinding.DataBindingComponentShortNamesCache"
                        && it::class.java.name != "com.android.tools.idea.databinding.DataBindingShortNamesCache"
                    }
                }
                finally {
                    myCachesField.isAccessible = previousIsAccessible
                }
            }
            catch (thr: Throwable) {
                // Our dirty hack isn't working
            }
        }

        return@lazy null
    }
    private fun getJavaCallables(name: String, shortNamesCache: PsiShortNamesCache): Sequence<Any> {
        filteredShortNamesCaches?.let { caches -> return getCallablesByName(name, scopeWithoutKotlin, caches) }
        return shortNamesCache.getFieldsByNameUnfiltered(name, scopeWithoutKotlin).asSequence() +
               shortNamesCache.getMethodsByNameUnfiltered(name, scopeWithoutKotlin).asSequence()
    }

    private fun getCallablesByName(name: String, scope: GlobalSearchScope, caches: List<PsiShortNamesCache>): Sequence<Any> {
        return caches.asSequence().flatMap { cache ->
            cache.getMethodsByNameUnfiltered(name, scope) + cache.getFieldsByNameUnfiltered(name, scope).asSequence()
        }
    }

    // getMethodsByName() removes duplicates from returned set of names, which can be excessively slow
    // if the number of candidates is large (KT-16071) and is unnecessary because Kotlin performs its own
    // duplicate filtering later
    private fun PsiShortNamesCache.getMethodsByNameUnfiltered(name: String, scope: GlobalSearchScope): Sequence<PsiMethod> {
        val result = arrayListOf<PsiMethod>()
        processMethodsWithName(name, scope) { result.add(it) }
        return result.asSequence()
    }

    private fun PsiShortNamesCache.getFieldsByNameUnfiltered(name: String, scope: GlobalSearchScope): Sequence<PsiField> {
        val result = arrayListOf<PsiField>()
        processFieldsWithName(name, { field -> result.add(field); true }, scope, null)
        return result.asSequence()
    }

    fun processKotlinCallablesByName(
            name: String,
            filter: (KtNamedDeclaration) -> Boolean,
            processor: (CallableDescriptor) -> Unit
    ) {
        val functions: Sequence<KtCallableDeclaration> = KotlinFunctionShortNameIndex.getInstance().get(name, project, scope).asSequence()
        val properties: Sequence<KtNamedDeclaration> = KotlinPropertyShortNameIndex.getInstance().get(name, project, scope).asSequence()
        val processed = HashSet<CallableDescriptor>()
        for (declaration in functions + properties) {
            ProgressManager.checkCanceled()
            if (!filter(declaration)) continue
            val descriptor = declaration.descriptor as? CallableDescriptor ?: continue
            if (!processed.add(descriptor)) continue
            if (!descriptorFilter(descriptor)) continue
            processor(descriptor)
        }
    }

    fun getKotlinClasses(
            nameFilter: (String) -> Boolean,
            psiFilter: (KtDeclaration) -> Boolean = { true },
            kindFilter: (ClassKind) -> Boolean = { true }): Collection<ClassDescriptor> {
        val index = KotlinFullClassNameIndex.getInstance()
        return index.getAllKeys(project).asSequence()
                .filter { fqName ->
                    ProgressManager.checkCanceled()
                    nameFilter(fqName.substringAfterLast('.'))
                }
                .toList()
                .flatMap { fqName ->
                    index[fqName, project, scope].flatMap { classOrObject ->
                        classOrObject.resolveToDescriptorsWithHack(psiFilter).filterIsInstance<ClassDescriptor>()
                    }
                }
                .filter { kindFilter(it.kind) && descriptorFilter(it) }
    }

    fun getTopLevelTypeAliases(nameFilter: (String) -> Boolean): Collection<TypeAliasDescriptor> {
        val index = KotlinTopLevelTypeAliasFqNameIndex.getInstance()
        return index.getAllKeys(project).asSequence()
                .filter {
                    ProgressManager.checkCanceled()
                    nameFilter(it.substringAfterLast('.'))
                }
                .toList()
                .flatMap { fqName ->
                    index[fqName, project, scope]
                            .flatMap { it.resolveToDescriptors<TypeAliasDescriptor>() }

                }
                .filter(descriptorFilter)
    }

    fun processObjectMembers(
            descriptorKindFilter: DescriptorKindFilter,
            nameFilter: (String) -> Boolean,
            filter: (KtNamedDeclaration, KtObjectDeclaration) -> Boolean,
            processor: (DeclarationDescriptor) -> Unit
    ) {
        fun processIndex(index: StringStubIndexExtension<out KtNamedDeclaration>) {
            for (name in index.getAllKeys(project)) {
                ProgressManager.checkCanceled()
                if (!nameFilter(name)) continue

                for (declaration in index.get(name, project, scope)) {
                    val objectDeclaration = declaration.parent.parent as? KtObjectDeclaration ?: continue
                    if (objectDeclaration.isObjectLiteral()) continue
                    if (filterOutPrivate && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) continue
                    if (!filter(declaration, objectDeclaration)) continue
                    for (descriptor in declaration.resolveToDescriptors<CallableDescriptor>()) {
                        if (descriptorKindFilter.accepts(descriptor) && descriptorFilter(descriptor)) {
                            processor(descriptor)
                        }
                    }
                }
            }
        }

        if (descriptorKindFilter.kindMask.and(DescriptorKindFilter.FUNCTIONS_MASK) != 0) {
            processIndex(KotlinFunctionShortNameIndex.getInstance())
        }
        if (descriptorKindFilter.kindMask.and(DescriptorKindFilter.VARIABLES_MASK) != 0) {
            processIndex(KotlinPropertyShortNameIndex.getInstance())
        }
    }

    fun processJavaStaticMembers(
            descriptorKindFilter: DescriptorKindFilter,
            nameFilter: (String) -> Boolean,
            processor: (DeclarationDescriptor) -> Unit
    ) {
        val idFilter = IdFilter.getProjectIdFilter(resolutionFacade.project, false)
        val shortNamesCache = PsiShortNamesCache.getInstance(project)

        val allMethodNames = hashSetOf<String>()
        shortNamesCache.processAllMethodNames({ name -> if (nameFilter(name)) allMethodNames.add(name); true }, scopeWithoutKotlin, idFilter)
        for (name in allMethodNames) {
            ProgressManager.checkCanceled()

            for (method in shortNamesCache.getMethodsByName(name, scopeWithoutKotlin).filterNot { it is KtLightElement<*, *> }) {
                if (!method.hasModifierProperty(PsiModifier.STATIC)) continue
                if (filterOutPrivate && method.hasModifierProperty(PsiModifier.PRIVATE)) continue
                if (method.containingClass?.parent !is PsiFile) continue // only top-level classes
                val descriptor = method.getJavaMethodDescriptor(resolutionFacade) ?: continue
                val container = descriptor.containingDeclaration as? ClassDescriptor ?: continue
                if (descriptorKindFilter.accepts(descriptor) && descriptorFilter(descriptor)) {
                    processor(descriptor)

                    // SAM-adapter
                    val syntheticScopes = resolutionFacade.getFrontendService(SyntheticScopes::class.java)
                    syntheticScopes.collectSyntheticStaticFunctions(container.staticScope, descriptor.name, NoLookupLocation.FROM_IDE)
                            .filterIsInstance<SamAdapterDescriptor<*>>()
                            .firstOrNull { it.baseDescriptorForSynthetic.original == descriptor.original }
                            ?.let { processor(it) }
                }
            }
        }

        val allFieldNames = hashSetOf<String>()
        shortNamesCache.processAllFieldNames({ name -> if (nameFilter(name)) allFieldNames.add(name); true }, scopeWithoutKotlin, idFilter)
        for (name in allFieldNames) {
            ProgressManager.checkCanceled()

            for (field in shortNamesCache.getFieldsByName(name, scopeWithoutKotlin).filterNot { it is KtLightElement<*, *> }) {
                if (!field.hasModifierProperty(PsiModifier.STATIC)) continue
                if (filterOutPrivate && field.hasModifierProperty(PsiModifier.PRIVATE)) continue
                val descriptor = field.getJavaFieldDescriptor() ?: continue
                if (descriptorKindFilter.accepts(descriptor) && descriptorFilter(descriptor)) {
                    processor(descriptor)
                }
            }
        }
    }

    private inline fun <reified TDescriptor : Any> KtNamedDeclaration.resolveToDescriptors(): Collection<TDescriptor> {
        return resolveToDescriptorsWithHack({ true }).filterIsInstance<TDescriptor>()
    }

    private fun KtNamedDeclaration.resolveToDescriptorsWithHack(
            psiFilter: (KtDeclaration) -> Boolean): Collection<DeclarationDescriptor> {
        if (containingKtFile.isCompiled) { //TODO: it's temporary while resolveToDescriptor does not work for compiled declarations
            return resolutionFacade.resolveImportReference(moduleDescriptor, fqName!!)
        }
        else {
            val translatedDeclaration = declarationTranslator(this) ?: return emptyList()
            if (!psiFilter(translatedDeclaration)) return emptyList()

            return listOfNotNull(resolutionFacade.resolveToDescriptor(translatedDeclaration))
        }
    }
}

