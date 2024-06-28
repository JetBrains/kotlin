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

package org.jetbrains.kotlin.codegen

import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ConflictingJvmDeclarationsData
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MemberKind
import org.jetbrains.kotlin.resolve.jvm.diagnostics.RawSignature
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor

abstract class SignatureCollectingClassBuilderFactory(
        delegate: ClassBuilderFactory, val shouldGenerate: (JvmDeclarationOrigin) -> Boolean
) : DelegatingClassBuilderFactory(delegate) {

    protected abstract fun handleClashingSignatures(data: ConflictingJvmDeclarationsData)
    protected abstract fun onClassDone(classOrigin: JvmDeclarationOrigin,
                                       classInternalName: String,
                                       signatures: MultiMap<RawSignature, JvmDeclarationOrigin>)

    override fun newClassBuilder(origin: JvmDeclarationOrigin): DelegatingClassBuilder {
        return SignatureCollectingClassBuilder(origin, delegate.newClassBuilder(origin))
    }

    private inner class SignatureCollectingClassBuilder(
            private val classCreatedFor: JvmDeclarationOrigin,
            internal val _delegate: ClassBuilder
    ) : DelegatingClassBuilder() {

        override fun getDelegate() = _delegate

        private lateinit var classInternalName: String

        private val signatures = MultiMap.createLinked<RawSignature, JvmDeclarationOrigin>()

        override fun defineClass(origin: PsiElement?, version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>) {
            classInternalName = name
            super.defineClass(origin, version, access, name, signature, superName, interfaces)
        }

        override fun newField(origin: JvmDeclarationOrigin, access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor {
            signatures.putValue(RawSignature(name, desc, MemberKind.FIELD), origin)
            if (!shouldGenerate(origin)) {
                return AbstractClassBuilder.EMPTY_FIELD_VISITOR
            }
            return super.newField(origin, access, name, desc, signature, value)
        }

        override fun newMethod(origin: JvmDeclarationOrigin, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            signatures.putValue(RawSignature(name, desc, MemberKind.METHOD), origin)
            if (!shouldGenerate(origin)) {
                return AbstractClassBuilder.EMPTY_METHOD_VISITOR
            }
            return super.newMethod(origin, access, name, desc, signature, exceptions)
        }

        override fun done(generateSmapCopyToAnnotation: Boolean) {
            for ((signature, elementsAndDescriptors) in signatures.entrySet()) {
                if (elementsAndDescriptors.size == 1) continue // no clash
                handleClashingSignatures(ConflictingJvmDeclarationsData(
                        classInternalName,
                        classCreatedFor,
                        signature,
                        elementsAndDescriptors,
                        elementsAndDescriptors.mapNotNull(JvmDeclarationOrigin::descriptor),
                ))
            }
            onClassDone(classCreatedFor, classInternalName, signatures)
            super.done(generateSmapCopyToAnnotation)
        }

    }
}
