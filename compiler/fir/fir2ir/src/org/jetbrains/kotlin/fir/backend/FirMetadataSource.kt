/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

sealed class FirMetadataSource : MetadataSource {
    abstract val fir: FirDeclaration

    val declarationSiteSession: FirSession
        get() = fir.moduleData.session

    override val name: Name?
        get() = when (val fir = fir) {
            is FirConstructor -> SpecialNames.INIT
            is FirSimpleFunction -> fir.name
            is FirRegularClass -> fir.name
            is FirProperty -> fir.name
            else -> null
        }

    class File(override val fir: FirFile) : FirMetadataSource(), MetadataSource.File {
        override var serializedIr: ByteArray? = null
    }

    class Class(override val fir: FirClass) : FirMetadataSource(), MetadataSource.Class {
        override var serializedIr: ByteArray? = null
    }

    class Function(override val fir: FirFunction) : FirMetadataSource(), MetadataSource.Function

    class Property(override val fir: FirProperty) : FirMetadataSource(), MetadataSource.Property {
        override val isConst: Boolean get() = fir.isConst
    }
}
