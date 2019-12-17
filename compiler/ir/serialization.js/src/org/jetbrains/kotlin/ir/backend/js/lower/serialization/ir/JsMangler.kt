/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.KotlinManglerImpl
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isInlined

abstract class AbstractJsMangler : KotlinManglerImpl() {
    override val IrType.isInlined: Boolean
        get() = this.isInlined()
}

object JsMangler : AbstractJsMangler()


/*
 * JsManglerForBE is a special verison of kotlin mangler used in case when IR is not completely correct from Type Parameter perspective.
 * I.e. usage of TypeParameter is not in TP's container. It's acceptable if only required to distinguish declaration somehow.
 */
object JsManglerForBE : AbstractJsMangler() {

    override fun mangleTypeParameter(typeParameter: IrTypeParameter, typeParameterNamer: (IrTypeParameter) -> String): String =
        typeParameter.name.asString()
}