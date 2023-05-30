/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.fir.backend.FirMangleComputer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.coneType

/**
 * JVM backend-specific mangle computer that generates a mangled name for a Kotlin declaration represented by [FirDeclaration].
 */
class FirJvmMangleComputer(
    builder: StringBuilder,
    mode: MangleMode,
) : FirMangleComputer(builder, mode) {
    override val visitor: Visitor = JvmVisitor()

    override fun FirFunction.platformSpecificSuffix(): String? =
        if (this is FirSimpleFunction && name.asString() == "main")
            this.moduleData.session.firProvider.getFirCallableContainerFile(symbol)?.name
        else null

    override fun addReturnType(): Boolean = true

    override fun copy(newMode: MangleMode): FirJvmMangleComputer =
        FirJvmMangleComputer(builder, newMode)

    // FIXME this implementation causes the mangler to deliver different result than IR mangler. Consider using base method instead
    override fun getEffectiveParent(typeParameter: ConeTypeParameterLookupTag): FirMemberDeclaration = typeParameter.symbol.fir.run {

        fun FirTypeParameter.sameAs(other: FirTypeParameter) =
            this === other ||
                    (name == other.name && bounds.size == other.bounds.size &&
                            bounds.zip(other.bounds).all { it.first.coneType == it.second.coneType })

        for (parent in typeParameterContainers) {
            if (parent.typeParameters.any { this.sameAs(it.symbol.fir) }) {
                return parent
            }
            if (parent is FirCallableDeclaration) {
                val overriddenFir = parent.originalForSubstitutionOverride
                if (overriddenFir is FirTypeParametersOwner && overriddenFir.typeParameters.any { this.sameAs(it) }) {
                    return parent
                }
            }
        }
        throw IllegalStateException("Should not be here!")
    }

    private inner class JvmVisitor : Visitor() {
        override fun visitField(field: FirField) {
            if (field is FirJavaField) {
                field.mangleSimpleDeclaration(field.name.asString())
            } else {
                visitVariable(field)
            }
        }
    }
}
