/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

abstract class FirModifierRenderer {
    internal lateinit var components: FirRendererComponents
    protected val printer get() = components.printer

    abstract fun renderModifiers(memberDeclaration: FirMemberDeclaration)
    abstract fun renderModifiers(backingField: FirBackingField)
    abstract fun renderModifiers(constructor: FirConstructor)
    abstract fun renderModifiers(propertyAccessor: FirPropertyAccessor)
    abstract fun renderModifiers(anonymousFunction: FirAnonymousFunction)
    open fun renderModifiers(valueParameter: FirValueParameter) {
        if (valueParameter.isCrossinline) {
            renderModifier("crossinline")
        }
        if (valueParameter.isNoinline) {
            renderModifier("noinline")
        }
        if (valueParameter.isVararg) {
            renderModifier("vararg")
        }
    }

    open fun renderModifiers(typeParameter: FirTypeParameter) {
        if (typeParameter.isReified) {
            renderModifier("reified")
        }
    }

    open fun renderModifiers(functionTypeRef: FirFunctionTypeRef) {
        if (functionTypeRef.isSuspend) {
            renderModifier("suspend")
        }
    }

    protected open fun renderModifier(modifier: String) {
        printer.print("$modifier ")
    }

    protected open fun Visibility.asString(effectiveVisibility: EffectiveVisibility? = null): String {
        val itself = when (this) {
            Visibilities.Unknown -> return "public?"
            else -> toString()
        }
        if (effectiveVisibility == null) return itself
        val effectiveAsVisibility = effectiveVisibility.toVisibility()
        if (effectiveAsVisibility == this) return itself
        if (effectiveAsVisibility == Visibilities.Private && this == Visibilities.PrivateToThis) return itself
        if (this !in visibilitiesToRenderEffectiveSet) return itself
        return itself + "[${effectiveVisibility.name}]"
    }

    protected open fun FirMemberDeclaration.modalityAsString(): String {
        return modality?.name?.toLowerCaseAsciiOnly() ?: run {
            if (this is FirCallableDeclaration && this.isOverride) {
                "open?"
            } else {
                "final?"
            }
        }
    }

    companion object {
        private val visibilitiesToRenderEffectiveSet = setOf(
            Visibilities.Private, Visibilities.PrivateToThis, Visibilities.Internal,
            Visibilities.Protected, Visibilities.Public, Visibilities.Local
        )
    }
}