/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.linkage.partial.IrUnimplementedOverridesStrategy
import org.jetbrains.kotlin.ir.linkage.partial.PartiallyLinkedDeclarationOrigin
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.collectRealOverrides
import org.jetbrains.kotlin.ir.util.isInterface

internal class ImplementAsErrorThrowingStubs(
    private val partialLinkageSupport: PartialLinkageSupportForLinker
) : IrUnimplementedOverridesStrategy {
    override fun <S : IrSymbol, T : IrOverridableDeclaration<S>> computeCustomization(overridableMember: T, parent: IrClass) =
        if (overridableMember.isAbstract
            && parent.isConcrete
            && parent.isEligibleForPartialLinkage()
            && !parent.delegatesToNothing
        ) {
            IrUnimplementedOverridesStrategy.Customization(
                origin = PartiallyLinkedDeclarationOrigin.UNIMPLEMENTED_ABSTRACT_CALLABLE_MEMBER,
                modality = parent.modality // Use modality of class for implemented callable member.
            )
        } else IrUnimplementedOverridesStrategy.Customization.NO

    override fun <S : IrSymbol, T : IrOverridableDeclaration<S>> postProcessGeneratedFakeOverride(overridableMember: T, parent: IrClass) {
        if (parent.isEligibleForPartialLinkage() && overridableMember.isAmbiguous()) {
            fun IrOverridableDeclaration<*>.mark() {
                if (isFakeOverride) {
                    origin = PartiallyLinkedDeclarationOrigin.AMBIGUOUS_NON_OVERRIDDEN_CALLABLE_MEMBER
                    isFakeOverride = false
                }
            }
            overridableMember.mark()
            if (overridableMember is IrProperty) {
                overridableMember.getter?.mark()
                overridableMember.setter?.mark()
            }
        }
    }

    /**
     * The function returns if fake override has unique implementation in super classes to be chosen on call
     *
     * Candidates is a list of real functions we override, which are not overridden themselves
     * by any other function in the list.
     *
     * If there is a **real** super-class function in the list, it must be unique.
     * In that case it is preferred over functions coming from default implementation in interfaces.
     *
     * If there is no such function, but there are several interface function - it is an incompatible change.
     *
     * This is done to mimic jvm behaviour.
     */
    private fun <S : IrSymbol, T : IrOverridableDeclaration<S>> T.isAmbiguous(): Boolean {
        val candidates = collectRealOverrides().filter { !it.isAbstract }
        if (candidates.any { ((it.symbol.owner as IrDeclaration).parent as? IrClass)?.isInterface == false }) {
            return false
        }
        return candidates.size > 1
    }

    private fun IrClass.isEligibleForPartialLinkage() = !isExternal && !partialLinkageSupport.shouldBeSkipped(this)


    private val IrOverridableMember.isAbstract: Boolean
        get() = modality == Modality.ABSTRACT

    private val IrClass.isConcrete: Boolean
        get() = modality != Modality.ABSTRACT && modality != Modality.SEALED

    private val IrClass.delegatesToNothing: Boolean
        get() = declarations.any { it is IrField && it.origin == IrDeclarationOrigin.DELEGATE && it.type.isNothing() }
}
