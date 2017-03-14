/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.asJava.builder

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
    override fun newClassBuilder(origin: JvmDeclarationOrigin) = StubClassBuilder(stubStack, javaFileStub)

    override fun asText(builder: ClassBuilder) = throw UnsupportedOperationException("asText is not implemented")
    override fun asBytes(builder: ClassBuilder) = throw UnsupportedOperationException("asBytes is not implemented")
    override fun close() {}

    fun result(): PsiJavaFileStub {
        val pop = stubStack.pop()
        if (pop !== javaFileStub) {
            LOG.error("Unbalanced stack operations: " + pop)
        }
        return javaFileStub
    }
}

private val LOG = Logger.getInstance(KotlinLightClassBuilderFactory::class.java)