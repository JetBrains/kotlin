/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.overrides.IrUnimplementedOverridesStrategy
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.collectRealOverrides
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal class ImplementAsErrorThrowingStubs(
    private val partialLinkageSupport: PartialLinkageSupportForLinker
) : IrUnimplementedOverridesStrategy {
    override fun postProcessGeneratedFakeOverride(overridableMember: IrOverridableDeclaration<*>, parent: IrClass) {
        if (!parent.isEligibleForPartialLinkage()) return
        val nonAbstractOverrides = overridableMember.collectRealOverrides { it.modality == Modality.ABSTRACT }

        val problem = when {
            nonAbstractOverrides.isEmpty() -> {
                runIf(!parent.delegatesToNothing && parent.modality != Modality.ABSTRACT && parent.modality != Modality.SEALED) {
                    PartiallyLinkedDeclarationOrigin.UNIMPLEMENTED_ABSTRACT_CALLABLE_MEMBER
                }
            }
            nonAbstractOverrides.size > 1 -> {
                /**
                 * The function returns if fake override has unique implementation in super classes to be chosen on call
                 *
                 * If there is a **real** super-class function in the list, it must be unique.
                 * In that case it is preferred over functions coming from the default implementation in interfaces.
                 *
                 * If there is no such function, but there are several interface functions - it is an incompatible change.
                 *
                 * This is done to mimic jvm behaviour.
                 */

                runIf(nonAbstractOverrides.none { it.parentClassOrNull?.isInterface == false }) {
                    PartiallyLinkedDeclarationOrigin.AMBIGUOUS_NON_OVERRIDDEN_CALLABLE_MEMBER
                }
            }
            else -> null
        } ?: return

        fun IrOverridableDeclaration<*>.mark() {
            if (isFakeOverride) {
                origin = problem
                isFakeOverride = false
            }
        }
        overridableMember.mark()
        if (overridableMember is IrProperty) {
            overridableMember.getter?.mark()
            overridableMember.setter?.mark()
        }
    }

    private fun IrClass.isEligibleForPartialLinkage() = !isExternal && !partialLinkageSupport.shouldBeSkipped(this)

    private val IrClass.delegatesToNothing: Boolean
        get() = declarations.any { it is IrField && it.origin == IrDeclarationOrigin.DELEGATE && it.type.isNothing() }
}
