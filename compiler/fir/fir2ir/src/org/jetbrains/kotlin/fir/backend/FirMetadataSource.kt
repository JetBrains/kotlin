/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.name.Name

interface FirMetadataSource : MetadataSource {

    val session: FirSession

    class File(
        val file: FirFile, override val session: FirSession
    ) : MetadataSource.File(emptyList()), FirMetadataSource

    class Class(
        val klass: FirClass<*>
    ) : FirMetadataSource {
        override val session: FirSession
            get() = klass.session

        override val name: Name?
            get() = (klass as? FirRegularClass)?.name
    }

    class Function(
        val function: FirFunction<*>
    ) : FirMetadataSource {
        override val session: FirSession
            get() = function.session

        override val name: Name?
            get() = when (function) {
                is FirSimpleFunction -> function.name
                is FirConstructor -> Name.special("<init>")
                else -> null
            }
    }

    class Property(
        val property: FirProperty
    ) : FirMetadataSource {
        override val session: FirSession
            get() = property.session

        override val name: Name
            get() = property.name
    }

    class Variable(
        val variable: FirVariable<*>
    ) : FirMetadataSource {
        override val session: FirSession
            get() = variable.session

        override val name: Name
            get() = variable.name
    }
}