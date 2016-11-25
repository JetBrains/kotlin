/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.project.Project
import com.intellij.psi.search.EverythingGlobalScope
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledTextIndexer
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.JvmBuiltInsSettings
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
        referencedDescriptor: DeclarationDescriptor
): KtDeclaration? {
    if (ErrorUtils.isError(referencedDescriptor)) return null
    if (isLocal(referencedDescriptor)) return null
    if (referencedDescriptor is PackageFragmentDescriptor || referencedDescriptor is PackageViewDescriptor) return null

    val decompiledFiles = findDecompiledFilesForDescriptor(project, referencedDescriptor)

    val referencedModule = referencedDescriptor.module
    return decompiledFiles.asSequence().mapNotNull { file ->
        val moduleInfo = file.getNullableModuleInfo()
        val libraryInfo = when (moduleInfo) {
            is LibraryInfo -> moduleInfo
            is LibrarySourceInfo -> LibraryInfo(project, moduleInfo.library)
            else -> null
        }
        val libraryModule = libraryInfo?.let { file.getResolutionFacade().findModuleDescriptor(it) }
        if (libraryModule != null
            && referencedModule.name != KotlinBuiltIns.BUILTINS_MODULE_NAME
            && referencedModule.name != libraryModule.name) return@mapNotNull null

        ByDescriptorIndexer.getDeclarationForDescriptor(referencedDescriptor, file)
    }.firstOrNull()
}

private fun isLocal(descriptor: DeclarationDescriptor): Boolean {
    if (descriptor is ParameterDescriptor) {
        return isLocal(descriptor.containingDeclaration)
    }
    else {
        return DescriptorUtils.isLocal(descriptor)
    }
}

private fun findDecompiledFilesForDescriptor(
        project: Project,
        referencedDescriptor: DeclarationDescriptor
): Collection<KtDecompiledFile> {
    return findCandidateDeclarationsInIndex(project, referencedDescriptor).mapNotNullTo(LinkedHashSet()) {
        it?.containingFile as? KtDecompiledFile
    }
}

private fun findCandidateDeclarationsInIndex(
        project: Project,
        referencedDescriptor: DeclarationDescriptor
): Collection<KtDeclaration?> {
    val scope = KotlinSourceFilterScope.libraryClassFiles(EverythingGlobalScope(project), project)

    val containingClass = DescriptorUtils.getParentOfType(referencedDescriptor, ClassDescriptor::class.java, false)
    if (containingClass != null) {
        return KotlinFullClassNameIndex.getInstance().get(containingClass.fqNameSafe.asString(), project, scope)
    }

    val topLevelDeclaration = DescriptorUtils.getParentOfType(referencedDescriptor, PropertyDescriptor::class.java, false)
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
                LOG.error("Parameter count mismatch for ${DescriptorRenderer.DEBUG_TEXT.render(callable)}[${original.index}] vs " +
                     callableDeclaration.valueParameterList?.text)
                return null
            }
            return callableDeclaration.valueParameters[original.index]
        }

        if (original is ConstructorDescriptor && original.isPrimary) {
            val classOrObject = getDeclarationForDescriptor(original.containingDeclaration, file) as? KtClassOrObject
            return classOrObject?.getPrimaryConstructor() ?: classOrObject
        }

        val descriptorKey = original.toStringKey()

        if (!file.isContentsLoaded && original is MemberDescriptor) {
            val hasDeclarationByKey = file.hasDeclarationWithKey(this, descriptorKey) ||
                                      (getBuiltinsDescriptorKey(descriptor)?.let { file.hasDeclarationWithKey(this, it) } ?: false)
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
        if (!JvmBuiltInsSettings.isSerializableInJava(classFqName)) return null

        val builtInDescriptor =
                DefaultBuiltIns.Instance.builtInsModule.resolveTopLevelClass(classFqName.toSafe(), NoLookupLocation.FROM_IDE)
        return builtInDescriptor?.toStringKey()
    }

    private fun DeclarationDescriptor.toStringKey(): String {
        return descriptorRendererForKeys.render(this)
    }

    private val descriptorRendererForKeys = DescriptorRenderer.COMPACT_WITH_MODIFIERS.withOptions {
        modifiers = DescriptorRendererModifier.ALL
        withDefinedIn = true
    }
}
