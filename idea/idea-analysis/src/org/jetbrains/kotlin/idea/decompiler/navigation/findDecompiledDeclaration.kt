/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.project.Project
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.project.BinaryModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.LOG
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledTextIndexer
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.types.ErrorUtils
import java.util.*

fun findDecompiledDeclaration(
    project: Project,
    referencedDescriptor: DeclarationDescriptor,
    // TODO: should not require explicitly specified scope to search for builtIns, use SourceElement to provide such information
    builtInsSearchScope: GlobalSearchScope?
): KtDeclaration? {
    if (ErrorUtils.isError(referencedDescriptor)) return null
    if (isLocal(referencedDescriptor)) return null
    if (referencedDescriptor is PackageFragmentDescriptor || referencedDescriptor is PackageViewDescriptor) return null

    val binaryInfo = referencedDescriptor.module.getCapability(ModuleInfo.Capability) as? BinaryModuleInfo

    binaryInfo?.binariesScope()?.let {
        return findInScope(referencedDescriptor, it)
    }
    if (KotlinBuiltIns.isBuiltIn(referencedDescriptor)) {
        // builtin module does not contain information about it's origin
        return builtInsSearchScope?.let { findInScope(referencedDescriptor, it) }
        // fallback on searching everywhere since builtIns are accessible from any context
            ?: findInScope(referencedDescriptor, GlobalSearchScope.allScope(project))
            ?: findInScope(referencedDescriptor, EverythingGlobalScope(project))
    }
    return null
}

private fun findInScope(referencedDescriptor: DeclarationDescriptor, scope: GlobalSearchScope): KtDeclaration? {
    val project = scope.project ?: return null
    val decompiledFiles = findCandidateDeclarationsInIndex(
        referencedDescriptor, KotlinSourceFilterScope.libraryClassFiles(scope, project), project
    ).mapNotNullTo(LinkedHashSet()) {
        it?.containingFile as? KtDecompiledFile
    }

    return decompiledFiles.asSequence().mapNotNull { file ->
        ByDescriptorIndexer.getDeclarationForDescriptor(referencedDescriptor, file)
    }.firstOrNull()
}

private fun isLocal(descriptor: DeclarationDescriptor): Boolean = if (descriptor is ParameterDescriptor) {
    isLocal(descriptor.containingDeclaration)
} else {
    DescriptorUtils.isLocal(descriptor)
}


private fun findCandidateDeclarationsInIndex(
    referencedDescriptor: DeclarationDescriptor,
    scope: GlobalSearchScope,
    project: Project
): Collection<KtDeclaration?> {
    val containingClass = DescriptorUtils.getParentOfType(referencedDescriptor, ClassDescriptor::class.java, false)
    if (containingClass != null) {
        return KotlinFullClassNameIndex.getInstance().get(containingClass.fqNameSafe.asString(), project, scope)
    }

    val topLevelDeclaration =
        DescriptorUtils.getParentOfType(referencedDescriptor, PropertyDescriptor::class.java, false) as DeclarationDescriptor?
            ?: DescriptorUtils.getParentOfType(referencedDescriptor, TypeAliasConstructorDescriptor::class.java, false)?.typeAliasDescriptor
            ?: DescriptorUtils.getParentOfType(referencedDescriptor, FunctionDescriptor::class.java, false)
            ?: DescriptorUtils.getParentOfType(referencedDescriptor, TypeAliasDescriptor::class.java, false)
            ?: return emptyList()

    // filter out synthetic descriptors
    if (!DescriptorUtils.isTopLevelDeclaration(topLevelDeclaration)) return emptyList()

    val fqName = topLevelDeclaration.fqNameSafe.asString()
    return when (topLevelDeclaration) {

        is FunctionDescriptor -> KotlinTopLevelFunctionFqnNameIndex.getInstance().get(fqName, project, scope)

        is PropertyDescriptor -> KotlinTopLevelPropertyFqnNameIndex.getInstance().get(fqName, project, scope)

        is TypeAliasDescriptor -> KotlinTopLevelTypeAliasFqNameIndex.getInstance().get(fqName, project, scope)

        else -> error("Referenced non local declaration that is not inside top level function, property, class or typealias:\n $referencedDescriptor")
    }
}

object ByDescriptorIndexer : DecompiledTextIndexer<String> {
    override fun indexDescriptor(descriptor: DeclarationDescriptor): Collection<String> {
        return listOf(descriptor.toStringKey())
    }

    internal fun getDeclarationForDescriptor(descriptor: DeclarationDescriptor, file: KtDecompiledFile): KtDeclaration? {
        val original = descriptor.original

        if (original is TypeAliasConstructorDescriptor) {
            return getDeclarationForDescriptor(original.typeAliasDescriptor, file)
        }

        if (original is ValueParameterDescriptor) {
            val callable = original.containingDeclaration
            val callableDeclaration = getDeclarationForDescriptor(callable, file) as? KtCallableDeclaration ?: return null
            if (original.index >= callableDeclaration.valueParameters.size) {
                LOG.error(
                    "Parameter count mismatch for ${DescriptorRenderer.DEBUG_TEXT.render(callable)}[${original.index}] vs " +
                            callableDeclaration.valueParameterList?.text
                )
                return null
            }
            return callableDeclaration.valueParameters[original.index]
        }

        if (original is ConstructorDescriptor && original.isPrimary) {
            val classOrObject = getDeclarationForDescriptor(original.containingDeclaration, file) as? KtClassOrObject
            return classOrObject?.primaryConstructor ?: classOrObject
        }

        val descriptorKey = original.toStringKey()

        if (!file.isContentsLoaded && original is MemberDescriptor) {
            val hasDeclarationByKey = file.hasDeclarationWithKey(this, descriptorKey) || (getBuiltinsDescriptorKey(descriptor)?.let {
                file.hasDeclarationWithKey(
                    this,
                    it
                )
            } ?: false)
            if (hasDeclarationByKey) {
                val declarationContainer: KtDeclarationContainer? = when {
                    DescriptorUtils.isTopLevelDeclaration(original) -> file
                    original.containingDeclaration is ClassDescriptor ->
                        getDeclarationForDescriptor(original.containingDeclaration as ClassDescriptor, file) as? KtClassOrObject
                    else -> null
                }

                if (declarationContainer != null) {
                    val descriptorName = original.name.asString()
                    val singleOrNull = declarationContainer.declarations.singleOrNull { it.name == descriptorName }
                    if (singleOrNull != null) {
                        return singleOrNull
                    }
                }
            }
        }

        return file.getDeclaration(this, descriptorKey) ?: run {
            return getBuiltinsDescriptorKey(descriptor)?.let { file.getDeclaration(this, it) }
        }
    }

    fun getBuiltinsDescriptorKey(descriptor: DeclarationDescriptor): String? {
        if (descriptor !is ClassDescriptor) return null

        val classFqName = descriptor.fqNameUnsafe
        if (!JvmBuiltInsSignatures.isSerializableInJava(classFqName)) return null

        val builtInDescriptor =
            DefaultBuiltIns.Instance.builtInsModule.resolveTopLevelClass(classFqName.toSafe(), NoLookupLocation.FROM_IDE)
        return builtInDescriptor?.toStringKey()
    }

    private fun DeclarationDescriptor.toStringKey(): String {
        return descriptorRendererForKeys.render(this)
    }

    private val descriptorRendererForKeys = DescriptorRenderer.withOptions {
        defaultDecompilerRendererOptions()
        withDefinedIn = true
    }
}
