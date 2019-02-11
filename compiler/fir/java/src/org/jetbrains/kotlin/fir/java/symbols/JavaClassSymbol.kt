/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.symbols

import org.jetbrains.kotlin.fir.symbols.ConeClassSymbol
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.ClassId

class JavaClassSymbol(javaClass: JavaClass) : ConeClassSymbol {
    override val classId: ClassId = javaClass.classId ?: error("!")
}


