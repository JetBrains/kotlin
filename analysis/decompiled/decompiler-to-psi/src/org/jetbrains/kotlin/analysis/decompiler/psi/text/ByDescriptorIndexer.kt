/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass


object ByDescriptorIndexer : DecompiledTextIndexer<String> {
    override fun indexDescriptor(descriptor: DeclarationDescriptor): Collection<String> {
        return listOf(descriptor.toStringKey())
    }

    fun getDeclarationForDescriptor(descriptor: DeclarationDescriptor, file: KtDecompiledFile): KtDeclaration? {
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

        if ((original as? CallableMemberDescriptor)?.mustNotBeWrittenToDecompiledText() == true &&
            original.containingDeclaration is ClassDescriptor
        ) {
            return getDeclarationForDescriptor(original.containingDeclaration, file)
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
        renderUnabbreviatedType = false
        defaultParameterValueRenderer = null
    }

    private val LOG = Logger.getInstance(this::class.java)
}
