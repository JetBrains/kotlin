/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies.semantics

import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.descriptors.isEnumEntry
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.resolve.dependencies.findCorrespondingEnumEntry
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

/**
 * Represents the sources of static entities such as properties in objects or top-level properties
 */
sealed interface EnclosingEntity<out D : FirDeclaration> {

    /**
     * Returns the symbol representing the entity's declaration
     */
    val symbol: FirBasedSymbol<D>

    /**
     * Returns the enclosing entity under which this entity is directly nested (declared), if applicable
     */
    val outerEnclosingEntity: EnclosingEntity<*>?

    data class Class(override val symbol: FirRegularClassSymbol) : EnclosingEntity<FirRegularClass> {
        override val outerEnclosingEntity: EnclosingEntity<*>? get() = null
        override fun toString(): String = "${symbol.name}::class"
    }

    data class Object(
        override val symbol: FirRegularClassSymbol,
        override val outerEnclosingEntity: Class? = null
    ) : EnclosingEntity<FirRegularClass> {
        override fun toString(): String = outerEnclosingEntity?.let { outerEnclosingEntity ->
            "$outerEnclosingEntity.${symbol.name}"
        } ?: "${symbol.name}"
    }

    data class File(override val symbol: FirFileSymbol) : EnclosingEntity<FirFile> {
        override val outerEnclosingEntity: EnclosingEntity<*>? get() = null
        override fun toString(): String = symbol.fir.name
    }

    data class EnumEntry(override val symbol: FirEnumEntrySymbol) : EnclosingEntity<FirEnumEntry> {
        override val outerEnclosingEntity: Class = symbol.getContainingClassSymbol()
            ?.fullyExpandedClass(symbol.moduleData.session)
            ?.asClassEntity()
            ?: error("An enum entry entity must always be nested under an enum class entity!")

        override fun toString(): String = "$outerEnclosingEntity.${symbol.name}"
    }

    data class InstancedProperty(
        override val symbol: FirPropertySymbol,
        override val outerEnclosingEntity: EnclosingEntity<*>
    ) : EnclosingEntity<FirProperty> {
        override fun toString(): String = "${if (outerEnclosingEntity is File) "" else "$outerEnclosingEntity."}${symbol.name}"
    }

    companion object {
        fun FirRegularClassSymbol.asObjectEntity(outerClass: Class? = null): Object? =
            when (classKind.isObject) {
                true -> Object(
                    this,
                    when (isCompanion) {
                        true if outerClass != null && getContainingClassSymbol() == outerClass.symbol -> outerClass
                        true -> getContainingClassSymbol()?.fullyExpandedClass(moduleData.session)?.asClassEntity()
                        false -> null
                    }
                )
                false -> null
            }

        fun FirRegularClassSymbol.asClassEntity(): Class? =
            when (classKind.isEnumClass || resolvedCompanionObjectSymbol != null) {
                true -> Class(this)
                false -> null
            }

        fun FirAnonymousObjectSymbol.asEnumEntryEntity(): EnumEntry? = findCorrespondingEnumEntry()?.let(::EnumEntry)

        fun FirClassLikeSymbol<*>.asEntity(): EnclosingEntity<*>? =
            when (this) {
                is FirRegularClassSymbol -> asObjectEntity() ?: asClassEntity()
                is FirAnonymousObjectSymbol -> asEnumEntryEntity()
                else -> null
            }

        fun FirFileSymbol.asFileEntity(): File = File(this)

        fun FirEnumEntrySymbol.asEnumEntryEntity(): EnumEntry = EnumEntry(this)

        fun FirPropertySymbol.asInstancedPropertyEntity(outerEnclosingEntity: EnclosingEntity<*>): InstancedProperty =
            InstancedProperty(this, outerEnclosingEntity)

        val EnclosingEntity<*>.outermostEntity: EnclosingEntity<*>
            get() = when (this) {
                is Class -> this
                is Object -> outerEnclosingEntity?.outermostEntity ?: this
                is File -> this
                is EnumEntry -> outerEnclosingEntity.outermostEntity
                is InstancedProperty -> outerEnclosingEntity.outermostEntity
            }
    }
}