/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.model

import org.jetbrains.kotlin.backend.common.dependencies.BeginStaticInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.ClinitIndex
import org.jetbrains.kotlin.backend.common.dependencies.EndStaticInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.EnumEntryIndex
import org.jetbrains.kotlin.backend.common.dependencies.QualifierIndex
import org.jetbrains.kotlin.backend.common.dependencies.TopLevelIndex
import org.jetbrains.kotlin.backend.common.dependencies.util.isPrivate
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.findIsInstanceAnd

sealed class EnclosingEntity<D : IrSymbolOwner> {

    abstract val symbol: IrBindableSymbol<*, D>

    abstract val name: FqName

    abstract val parentEnclosingEntity: EnclosingEntity<*>?

    abstract val isPrivate: Boolean

    abstract val containingFile: IrFile?

    abstract val beginInitializationIndex: BeginStaticInitializationIndex<D>

    val endInitializationIndex: EndStaticInitializationIndex<D> = EndStaticInitializationIndex(this)

    override fun toString(): String = name.asString()

    data class Class(override val symbol: IrClassSymbol) : EnclosingEntity<IrClass>() {

        override val name: FqName = symbol.owner.classId?.relativeClassName ?: FqName.topLevel(Name.special("<anonymous>"))

        override val parentEnclosingEntity: EnclosingEntity<*>? = null

        override val isPrivate: Boolean = symbol.isPrivate

        override val containingFile: IrFile? = symbol.owner.fileOrNull

        override val beginInitializationIndex: ClinitIndex = ClinitIndex(this)
    }

    data class Object(override val symbol: IrClassSymbol) : EnclosingEntity<IrClass>() {

        override val name: FqName = symbol.owner.classIdOrFail.relativeClassName

        override val parentEnclosingEntity: Class? = symbol.owner.takeIf(IrClass::isCompanion)
            ?.parentClassOrNull?.symbol?.asClassEntity()

        override val isPrivate: Boolean = symbol.isPrivate

        override val containingFile: IrFile? = symbol.owner.fileOrNull

        override val beginInitializationIndex: QualifierIndex = QualifierIndex(this)

        val isCompanion: Boolean = parentEnclosingEntity != null
    }

    data class EnumEntry(override val symbol: IrEnumEntrySymbol) : EnclosingEntity<IrEnumEntry>() {

        override val name: FqName = symbol.owner.let { it.parentAsClass.classIdOrFail.relativeClassName.child(it.name) }

        override val parentEnclosingEntity: Class = symbol.owner.parentAsClass.symbol.asClassEntity()

        override val isPrivate: Boolean = false

        override val containingFile: IrFile? = symbol.owner.fileOrNull

        override val beginInitializationIndex: EnumEntryIndex = EnumEntryIndex(this)
    }

    data class File(override val symbol: IrFileSymbol) : EnclosingEntity<IrFile>() {

        override val name: FqName = symbol.owner.packageFqName.child(Name.identifier(symbol.owner.name))

        override val parentEnclosingEntity: EnclosingEntity<*>? = null

        override val isPrivate: Boolean = false

        override val containingFile: IrFile = symbol.owner

        override val beginInitializationIndex: TopLevelIndex = TopLevelIndex(this)
    }

    companion object {

        val EnclosingEntity<*>.isNotPrivate: Boolean get() = !isPrivate

        val EnclosingEntity<*>.parentEnclosingEntityOrSelf: EnclosingEntity<*> get() = parentEnclosingEntity ?: this

        fun IrClassSymbol.asObjectEntity(): Object? = when {
            owner.kind.isObject -> Object(this)
            else -> null
        }

        fun IrClass.asObjectEntity(): Object? = symbol.asObjectEntity()

        fun IrClassSymbol.asClassEntity(): Class = Class(this)

        fun IrClass.asClassEntity(): Class = symbol.asClassEntity()

        fun IrBindableSymbol<*, *>.asEntity(allowClass: Boolean = true): EnclosingEntity<*>? =
            when (this) {
                is IrClassSymbol -> asObjectEntity()
                    ?: run {
                        owner.parentClassOrNull?.takeIf(IrClass::isEnumClass)
                            ?.declarations?.findIsInstanceAnd<IrEnumEntry> { it.correspondingClass == this }
                            ?.symbol?.asEnumEntryEntity()
                    } ?: if (allowClass) asClassEntity() else null
                is IrFileSymbol -> asFileEntity()
                else -> null
            }

        fun IrFileSymbol.asFileEntity(): File = File(this)

        fun IrFile.asFileEntity(): File = symbol.asFileEntity()

        fun IrEnumEntrySymbol.asEnumEntryEntity(): EnumEntry = EnumEntry(this)

        fun IrEnumEntry.asEnumEntryEntity(): EnumEntry = symbol.asEnumEntryEntity()
    }
}
