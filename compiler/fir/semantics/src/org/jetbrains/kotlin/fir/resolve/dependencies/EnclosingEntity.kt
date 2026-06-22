/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.withSession

sealed class EnclosingEntity<D : FirDeclaration> {

    abstract val symbol: FirBasedSymbol<D>

    abstract val parentEnclosingEntity: EnclosingEntity<*>?

    abstract val beginInitializationIndex: BeginInitializationIndex<D>

    open val isPrivate: Boolean get() = false

    val endInitializationIndex: EndInitializationIndex<D> by lazy { EndInitializationIndex(beginInitializationIndex) }

    data class Class(override val symbol: FirRegularClassSymbol) : EnclosingEntity<FirRegularClass>() {

        override val parentEnclosingEntity: EnclosingEntity<*>? get() = null

        override val beginInitializationIndex: ClinitIndex = ClinitIndex(this)

        override val isPrivate: Boolean = symbol.isPrivate

        override fun toString(): String = symbol.classId.relativeClassName.asString()
    }

    data class Object(
        override val symbol: FirRegularClassSymbol,
        override val parentEnclosingEntity: Class? = null
    ) : EnclosingEntity<FirRegularClass>() {

        override val beginInitializationIndex: QualifierIndex = QualifierIndex(this)

        override val isPrivate: Boolean = symbol.isPrivate

        override fun toString(): String = parentEnclosingEntity?.let { outerEnclosingEntity ->
            "$outerEnclosingEntity.${symbol.name}"
        } ?: "${symbol.name}"
    }

    data class EnumEntry(override val symbol: FirEnumEntrySymbol) : EnclosingEntity<FirEnumEntry>() {
        override val parentEnclosingEntity: Class = symbol.getContainingClassSymbol()
            ?.fullyExpandedClass(symbol.moduleData.session)
            ?.asClassEntity()
            ?: error("An enum entry entity must always be nested under an enum class entity!")

        override val beginInitializationIndex: EnumEntryIndex = EnumEntryIndex(this)

        override val isPrivate: Boolean get() = parentEnclosingEntity.isPrivate

        override fun toString(): String = "${symbol.callableId.className?.asString() ?: ""}.${symbol.name}"
    }

    data class File(override val symbol: FirFileSymbol) : EnclosingEntity<FirFile>() {

        override val parentEnclosingEntity: EnclosingEntity<*>? get() = null

        override val beginInitializationIndex: TopLevelIndex = TopLevelIndex(this)

        override fun toString(): String = symbol.fir.name
    }

    companion object {

        val EnclosingEntity<*>.isNotPrivate: Boolean get() = !isPrivate

        val EnclosingEntity<*>.parentEnclosingEntityOrSelf: EnclosingEntity<*> get() = parentEnclosingEntity ?: this

        context(sessionHolder: SessionHolder)
        fun FirRegularClassSymbol.asObjectEntity(
            outerClass: Class? = when {
                isCompanion -> getContainingClassSymbol()?.fullyExpandedClass()?.asClassEntity()
                else -> null
            }
        ): Object? = when {
            classKind.isObject -> {
                outerClass?.let { require(it.symbol == getContainingClassSymbol()) } ?: require(!isCompanion)
                Object(this, outerClass)
            }
            else -> null
        }

        fun FirRegularClassSymbol.asClassEntity(): Class = Class(this)

        context(sessionHolder: SessionHolder)
        fun FirAnonymousObjectSymbol.getParentEnumClassEntity(): Class? =
            findCorrespondingEnumEntry()?.first?.asClassEntity()

        context(_: SessionHolder)
        fun FirBasedSymbol<*>.asEntity(allowClass: Boolean = true): EnclosingEntity<*>? =
            when (this) {
                is FirRegularClassSymbol -> asObjectEntity() ?: if (allowClass) asClassEntity() else null
                is FirAnonymousObjectSymbol -> getParentEnumClassEntity()
                is FirFileSymbol -> asFileEntity()
                else -> null
            }

        fun FirBasedSymbol<*>.asEntity(session: FirSession, allowClass: Boolean = true): EnclosingEntity<*>? =
            withSession(session) { asEntity(allowClass) }

        fun FirFileSymbol.asFileEntity(): File = File(this)

        fun FirEnumEntrySymbol.asEnumEntryEntity(): EnumEntry = EnumEntry(this)
    }
}
