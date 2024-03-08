/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.builder

import org.jetbrains.kotlin.codegen.AbstractClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter

object KotlinLightClassBuilderFactory : ClassBuilderFactory {
    override fun getClassBuilderMode(): ClassBuilderMode = ClassBuilderMode.LIGHT_CLASSES
    override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder =
        object : AbstractClassBuilder() {
            override fun getVisitor(): ClassVisitor = ClassWriter(0)
        }

    override fun asText(builder: ClassBuilder) = throw UnsupportedOperationException("asText is not implemented")
    override fun asBytes(builder: ClassBuilder) = throw UnsupportedOperationException("asBytes is not implemented")
    override fun close() {}
}
