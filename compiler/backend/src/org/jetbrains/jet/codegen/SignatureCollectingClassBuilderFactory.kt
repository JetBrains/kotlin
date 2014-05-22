/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import com.intellij.util.containers.MultiMap

public enum class MemberKind { FIELD; METHOD }
public data class RawSignature(val name: String, val desc: String, val field: MemberKind)

public class JvmDeclarationOrigin(
        val element: PsiElement?,
        val descriptor: DeclarationDescriptor?
)

public val NO_ORIGIN: JvmDeclarationOrigin = JvmDeclarationOrigin(null, null)

public abstract class SignatureCollectingClassBuilderFactory(
        private val delegate: ClassBuilderFactory

) : ClassBuilderFactory by delegate {

    protected abstract fun handleClashingSignatures(
            classInternalName: String?,
            classOrigin: JvmDeclarationOrigin,
            signature: RawSignature,
            origins: Collection<JvmDeclarationOrigin>
    )

    override fun newClassBuilder(forElement: PsiElement?, forDescriptor: DeclarationDescriptor?): SignatureCollectingClassBuilder {
        return SignatureCollectingClassBuilder(JvmDeclarationOrigin(forElement, forDescriptor), delegate.newClassBuilder(forElement, forDescriptor))
    }

    public override fun asBytes(builder: ClassBuilder?): ByteArray? {
        return delegate.asBytes((builder as SignatureCollectingClassBuilder)._delegate)
    }

    private inner class SignatureCollectingClassBuilder(
            private val classCreatedFor: JvmDeclarationOrigin,
            internal val _delegate: ClassBuilder
    ) : DelegatingClassBuilder() {

        override fun getDelegate() = _delegate

        private var classInternalName: String? = null

        private val signatures = MultiMap<RawSignature, JvmDeclarationOrigin>()

        override fun defineClass(origin: PsiElement?, version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>) {
            classInternalName = name
            super.defineClass(origin, version, access, name, signature, superName, interfaces)
        }

        override fun newField(origin: PsiElement?, descriptor: DeclarationDescriptor?, access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor {
            signatures.putValue(RawSignature(name, desc, MemberKind.FIELD), JvmDeclarationOrigin(origin, descriptor))
            return super.newField(origin, descriptor, access, name, desc, signature, value)
        }

        override fun newMethod(origin: PsiElement?, descriptor: DeclarationDescriptor?, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            signatures.putValue(RawSignature(name, desc, MemberKind.METHOD), JvmDeclarationOrigin(origin, descriptor))
            return super.newMethod(origin, descriptor, access, name, desc, signature, exceptions)
        }

        override fun done() {
            for ((signature, elementsAndDescriptors) in signatures.entrySet()!!) {
                if (elementsAndDescriptors.size == 1) continue // no clash
                handleClashingSignatures(
                        classInternalName,
                        classCreatedFor,
                        signature,
                        elementsAndDescriptors
                )
            }
            super.done()
        }

    }
}
