/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.toEffectiveVisibilityOrNull
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrTypeTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Verifies that all expressions and types that reference declarations, the referenced declaration is actually visible in that scope.
 *
 * This is a simplified version that only checks that:
 * - If the referenced declaration is private, it's located in the same file as the reference itself.
 * - If the referenced declaration is internal, it's located in the same module as the reference itself, or in a friend module.
 * - No referenced declaration has unknown visibility.
 *
 * Anything more complex is not really needed, since in K/JVM we have the JVM itself for checking visibilities.
 * And in KLIB-based backends, checking references to private declarations is only important in the context of incremental compilation,
 * which operates on the file level, not on the declaration level.
 *
 * **Note**: this checker intentionally doesn't check visibilities of annotations, since annotations don't participate in the execution
 * process on KLIB-based backends.
 * Moreover, the fact that annotations are represented as [IrConstructorCall]s is an (unfortunate) implementation detail.
 * The only important pieces of information in an annotation are its fully qualified name and arguments.
 * Visibility is irrelevant.
 */
internal class IrVisibilityChecker(
    private val module: IrModuleFragment,
    private val file: IrFile,
    private val reportError: ReportIrValidationError,
) : IrTypeTransformerVoid() {

    private val parentChain = mutableListOf<IrElement>()

    private fun visibilityError(element: IrElement, visibility: Visibility) {
        val message = "The following element references " +
                if (visibility == Visibilities.Unknown) {
                    "a declaration with unknown visibility:"
                } else {
                    "'${visibility.name}' declaration that is invisible in the current scope:"
                }
        reportError(
            file,
            element,
            message,
            parentChain,
        )
    }

    override fun visitElement(element: IrElement) {
        parentChain.push(element)
        element.acceptChildrenVoid(this)
        parentChain.pop()
    }

    private fun IrDeclarationWithVisibility.isVisibleAsInternal(): Boolean {
        val referencedDeclarationPackageFragment = getPackageFragment()
        if (referencedDeclarationPackageFragment.symbol is DescriptorlessExternalPackageFragmentSymbol) {
            // When compiling JS stdlib, intrinsic declarations are moved to a special module that doesn't have a descriptor.
            // This happens after deserialization but before executing any lowerings, including IR validating lowering
            // See MoveBodilessDeclarationsToSeparatePlaceLowering
            return this@IrVisibilityChecker.module.name.asString() == "<kotlin>"
        }
        return this@IrVisibilityChecker.module.descriptor.shouldSeeInternalsOf(referencedDeclarationPackageFragment.moduleDescriptor)
    }

    private fun IrDeclarationWithVisibility.isVisibleAsPrivate(): Boolean {
        // We're comparing file entries instead of files themselves because on JS
        // MoveBodilessDeclarationsToSeparatePlaceLowering performs shallow copying of IrFiles for some reason
        return this@IrVisibilityChecker.file.fileEntry == fileOrNull?.fileEntry
    }

    private fun checkVisibility(
        referencedDeclarationSymbol: IrSymbol,
        reference: IrElement,
    ) {
        val referencedDeclaration = referencedDeclarationSymbol.owner as? IrDeclarationWithVisibility ?: return
        val classOfReferenced = referencedDeclaration.parentClassOrNull
        val visibility = referencedDeclaration.visibility.delegate

        fun IrAnnotationContainer.isPublishedApi() = hasAnnotation(StandardClassIds.Annotations.PublishedApi)

        val effectiveVisibility = visibility.toEffectiveVisibilityOrNull(
            container = classOfReferenced?.symbol,
            forClass = true,
            ownerIsPublishedApi = referencedDeclaration.run {
                isPublishedApi() || this is IrSimpleFunction && correspondingPropertySymbol?.owner?.isPublishedApi() == true
            }
        )

        val isVisible = when (effectiveVisibility) {
            is EffectiveVisibility.Internal, is EffectiveVisibility.InternalProtected, is EffectiveVisibility.InternalProtectedBound ->
                referencedDeclaration.isVisibleAsInternal()
            is EffectiveVisibility.Local, is EffectiveVisibility.PrivateInClass, is EffectiveVisibility.PrivateInFile ->
                referencedDeclaration.isVisibleAsPrivate()
            is EffectiveVisibility.PackagePrivate, is EffectiveVisibility.Protected, is EffectiveVisibility.ProtectedBound, is EffectiveVisibility.Public -> true
            is EffectiveVisibility.Unknown, null -> false // We shouldn't encounter unknown visibilities at this point
        }

        if (!isVisible) {
            visibilityError(reference, visibility)
        }
    }

    private fun List<IrTypeArgument>.checkVisibilities(element: IrElement) {
        for (argument in this) {
            (argument as? IrTypeProjection)?.type?.checkVisibilitiesInType(element)
        }
    }

    private fun IrType.checkVisibilitiesInType(element: IrElement) {
        if (this is IrSimpleType) {
            checkVisibility(classifier, element)
            arguments.checkVisibilities(element)
        }
    }

    override fun <Type : IrType?> transformType(container: IrElement, type: Type): Type =
        type.also { it?.checkVisibilitiesInType(container) }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        checkVisibility(expression.symbol, expression)
        super.visitDeclarationReference(expression)
    }
}