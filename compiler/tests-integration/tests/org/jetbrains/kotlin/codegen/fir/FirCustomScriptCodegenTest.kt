package org.jetbrains.kotlin.codegen.fir

import org.jetbrains.kotlin.codegen.ir.IrCustomScriptCodegenTest
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.FirParser.LightTree
import org.jetbrains.kotlin.test.FirParser.Psi

class FirLightTreeCustomScriptCodegenTest : IrCustomScriptCodegenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirPsiCustomScriptCodegenTest : IrCustomScriptCodegenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}