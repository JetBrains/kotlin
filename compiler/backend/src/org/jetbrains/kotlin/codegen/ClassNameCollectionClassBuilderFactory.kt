/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ConflictingJvmDeclarationsData
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MemberKind
import org.jetbrains.kotlin.resolve.jvm.diagnostics.RawSignature
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor

public abstract class ClassNameCollectionClassBuilderFactory(
        private val delegate: ClassBuilderFactory

) : ClassBuilderFactory by delegate {

    protected abstract fun handleClashingNames(internalName: String, origin: JvmDeclarationOrigin)

    override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassNameCollectionClassBuilder {
        return ClassNameCollectionClassBuilder(origin, delegate.newClassBuilder(origin))
    }

    public override fun asBytes(builder: ClassBuilder?): ByteArray? {
        return delegate.asBytes((builder as ClassNameCollectionClassBuilder)._delegate)
    }

    public override fun asText(builder: ClassBuilder?): String? {
        return delegate.asText((builder as ClassNameCollectionClassBuilder)._delegate)
    }

    public override fun close() {
        delegate.close()
    }

    private inner class ClassNameCollectionClassBuilder(
            private val classCreatedFor: JvmDeclarationOrigin,
            internal val _delegate: ClassBuilder
    ) : DelegatingClassBuilder() {

        override fun getDelegate() = _delegate

        private var classInternalName: String? = null

        override fun defineClass(origin: PsiElement?, version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>) {
            classInternalName = name
            super.defineClass(origin, version, access, name, signature, superName, interfaces)
        }

        override fun done() {
            if (classInternalName != null) {
                handleClashingNames(classInternalName!!, classCreatedFor)
            }
            super.done()
        }
    }
}
