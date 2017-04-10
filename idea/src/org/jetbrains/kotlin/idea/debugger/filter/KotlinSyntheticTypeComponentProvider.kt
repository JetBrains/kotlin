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

package org.jetbrains.kotlin.idea.debugger.filter

import com.intellij.debugger.engine.SyntheticTypeComponentProvider
import com.sun.jdi.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.org.objectweb.asm.Opcodes
import kotlin.jvm.internal.FunctionReference
import kotlin.jvm.internal.PropertyReference

class KotlinSyntheticTypeComponentProvider: SyntheticTypeComponentProvider {
    override fun isSynthetic(typeComponent: TypeComponent?): Boolean {
        if (typeComponent !is Method) return false

        val containingType = typeComponent.declaringType()
        val typeName = containingType.name()
        if (!FqNameUnsafe.isValid(typeName)) return false

        if (containingType.isCallableReferenceSyntheticClass()) {
            return true
        }

        try {
            if (typeComponent.isDelegateToDefaultInterfaceImpl()) return true

            if (typeComponent.location().lineNumber() != 1) return false

            if (typeComponent.allLineLocations().any { it.lineNumber() != 1 }) {
                return false
            }

            return !typeComponent.declaringType().allLineLocations().any { it.lineNumber() != 1 }
        }
        catch(e: AbsentInformationException) {
            return false
        }
        catch(e: UnsupportedOperationException) {
            return false
        }
    }

    private tailrec fun ReferenceType?.isCallableReferenceSyntheticClass(): Boolean {
        if (this !is ClassType) return false
        val superClass = this.superclass() ?: return false
        val superClassName = superClass.name()
        if (superClassName == PropertyReference::class.java.name || superClassName == FunctionReference::class.java.name) {
            return true
        }

        // The direct supertype may be PropertyReference0 or something
        return if (superClassName.startsWith("kotlin.jvm.internal."))
            superClass.isCallableReferenceSyntheticClass()
        else
            false
    }

    private fun Method.isDelegateToDefaultInterfaceImpl(): Boolean {
        if (allLineLocations().size != 1) return false
        if (!virtualMachine().canGetBytecodes()) return false

        if (!hasOnlyInvokeStatic(this)) return false

        return hasInterfaceWithImplementation(this)
    }

    private val LOAD_INSTRUCTIONS_WITH_INDEX = Opcodes.ILOAD.toByte()..Opcodes.ALOAD.toByte()
    private val LOAD_INSTRUCTIONS = (Opcodes.ALOAD + 1).toByte()..(Opcodes.IALOAD - 1).toByte()

    private val RETURN_INSTRUCTIONS = Opcodes.IRETURN.toByte()..Opcodes.RETURN.toByte()

    // Check that method contains only load and invokeStatic instructions. Note that if after load goes ldc instruction it could be checkParametersNotNull method invocation
    private fun hasOnlyInvokeStatic(m: Method): Boolean {
        val bytecodes = m.bytecodes()
        var i = 0
        var isALoad0BeforeStaticCall = false
        while (i < bytecodes.size) {
            val instr = bytecodes[i]
            when {
                instr == 42.toByte() /* ALOAD_0 */ -> {
                    i += 1
                    isALoad0BeforeStaticCall = true
                }
                instr in LOAD_INSTRUCTIONS_WITH_INDEX || instr in LOAD_INSTRUCTIONS -> {
                    i += 1
                    if (instr in LOAD_INSTRUCTIONS_WITH_INDEX) i += 1
                    val nextInstr = bytecodes[i]
                    if (nextInstr == Opcodes.LDC.toByte()) {
                        i += 2
                        isALoad0BeforeStaticCall = false
                    }
                }
                instr == Opcodes.INVOKESTATIC.toByte() -> {
                    i += 3
                    if (isALoad0BeforeStaticCall && i == (bytecodes.size - 1)) {
                        val nextInstr = bytecodes[i]
                        return nextInstr in RETURN_INSTRUCTIONS
                    }
                }
                else -> return false
            }
        }
        return false
    }

    // TODO: class DefaultImpl can be not loaded
    private fun hasInterfaceWithImplementation(method: Method): Boolean {
        val declaringType = method.declaringType() as? ClassType ?: return false
        val interfaces = declaringType.allInterfaces()
        val vm = declaringType.virtualMachine()
        val traitImpls = interfaces.flatMap { vm.classesByName(it.name() + JvmAbi.DEFAULT_IMPLS_SUFFIX) }
        return traitImpls.any { !it.methodsByName(method.name()).isEmpty() }
    }
}