/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.Importable

data class Type(override val packageName: String?, override val type: String) : Importable {
    override fun getTypeWithArguments(notNull: Boolean): String = type
}
