/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.fir.symbols.ConeClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FictitiousFunctionSymbol(className: Name, arity: Int) : ConeClassSymbol {
    override val classId = ClassId(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, className)

    class TypeParameterSymbol(index: Int) : ConeTypeParameterSymbol {
        override val name: Name = Name.identifier("T$index")
    }

}