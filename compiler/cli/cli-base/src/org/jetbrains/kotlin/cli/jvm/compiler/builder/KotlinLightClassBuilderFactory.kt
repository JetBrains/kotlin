/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.builder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

class KotlinLightClassBuilderFactory(private val javaFileStub: PsiJavaFileStub) : ClassBuilderFactory {
    private val stubStack = Stack<StubElement<PsiElement>>().apply {
        @Suppress("UNCHECKED_CAST")
        push(javaFileStub as StubElement<PsiElement>)
    }

    override fun getClassBuilderMode(): ClassBuilderMode = ClassBuilderMode.LIGHT_CLASSES
    override fun newClassBuilder(origin: JvmDeclarationOrigin) =
        StubClassBuilder(stubStack, javaFileStub)

    override fun asText(builder: ClassBuilder) = throw UnsupportedOperationException("asText is not implemented")
    override fun asBytes(builder: ClassBuilder) = throw UnsupportedOperationException("asBytes is not implemented")
    override fun close() {}

    fun result(): PsiJavaFileStub {
        val pop = stubStack.pop()
        if (pop !== javaFileStub) {
            LOG.error("Unbalanced stack operations: $pop")
        }

        return javaFileStub
    }
}

private val LOG = Logger.getInstance(KotlinLightClassBuilderFactory::class.java)
