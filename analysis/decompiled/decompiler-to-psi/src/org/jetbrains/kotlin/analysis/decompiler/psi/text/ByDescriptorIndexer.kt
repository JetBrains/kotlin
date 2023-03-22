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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.types.KotlinType


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

        if (!file.isContentsLoaded && original is MemberDescriptor) {
            val declarationContainer: KtDeclarationContainer? = when {
                DescriptorUtils.isTopLevelDeclaration(original) -> file
                original.containingDeclaration is ClassDescriptor ->
                    getDeclarationForDescriptor(original.containingDeclaration as ClassDescriptor, file) as? KtClassOrObject
                else -> null
            }

            if (declarationContainer != null) {
                val descriptorName = original.name.asString()
                val firstOrNull = declarationContainer.declarations
                    .filter { it.name == descriptorName }
                    .firstOrNull { declaration ->
                        if (original is FunctionDescriptor) {
                            declaration is KtFunction && isSameFunction(declaration, original)
                        } else true
                    }
                return firstOrNull
            }
        }

        error("Should not be reachable")
    }

    private fun isSameFunction(
        declaration: KtFunction,
        original: FunctionDescriptor
    ): Boolean {
        if (declaration.valueParameters.size != original.valueParameters.size) {
            return false
        }

        val ktTypeReference = declaration.receiverTypeReference
        val receiverParameter = original.extensionReceiverParameter
        if (ktTypeReference != null) {
            if (receiverParameter == null) return false
            val receiverType = receiverParameter.type
            if (!isPotentiallySameType(receiverType, ktTypeReference)) {
                return false
            }
        } else if (receiverParameter != null) return false


        declaration.valueParameters.zip(original.valueParameters).forEach { (ktParam, paramDesc) ->
            if (!isPotentiallySameType(paramDesc.type, ktParam.typeReference!!)) {
                return false
            }
        }
        return true
    }

    private fun isPotentiallySameType(
        kotlinType: KotlinType,
        ktTypeReference: KtTypeReference
    ): Boolean {
        return kotlinType.constructor.declarationDescriptor?.name?.identifier == (ktTypeReference.typeElement as? KtUserType)?.referencedName
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
