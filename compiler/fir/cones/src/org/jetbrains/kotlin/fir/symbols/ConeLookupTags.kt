/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class ConeClassifierLookupTag {
    abstract val name: Name

    override fun toString(): String {
        return name.asString()
    }
}

abstract class ConeClassLikeLookupTag : ConeClassifierLookupTag() {
    abstract val classId: ClassId

    override val name: Name
        get() = classId.shortClassName
}

