/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import fleet.com.intellij.platform.syntax.SyntaxElementType

class NewParser : AbstractParser<SyntaxElementType>() {
    override fun parse(fileName: String, text: String): TestParseNode<SyntaxElementType> {
        TODO("Implement new parser (KT-77144)")
    }
}