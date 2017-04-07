/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.types.KotlinType

/**
 * Provides utilities to create static data.
 */
internal class StaticData(override val context: Context): ContextUtils {

    /**
     * Represents the LLVM global variable.
     */
    class Global private constructor(val staticData: StaticData, val llvmGlobal: LLVMValueRef) {
        companion object {

            private fun createLlvmGlobal(module: LLVMModuleRef,
                                         type: LLVMTypeRef,
                                         name: String,
                                         isExported: Boolean
            ): LLVMValueRef {

                if (isExported && LLVMGetNamedGlobal(module, name) != null) {
                    throw IllegalArgumentException("Global '$name' already exists")
                }

                // Globals created with this API are *not* thread local.
                val llvmGlobal = LLVMAddGlobal(module, type, name)!!

                if (!isExported) {
                    LLVMSetLinkage(llvmGlobal, LLVMLinkage.LLVMInternalLinkage)
                }

                return llvmGlobal
            }

            fun create(staticData: StaticData, type: LLVMTypeRef, name: String, isExported: Boolean): Global {
                val module = staticData.context.llvmModule

                val isUnnamed = (name == "") // LLVM will select the unique index and represent the global as `@idx`.
                if (isUnnamed && isExported) {
                    throw IllegalArgumentException("unnamed global can't be exported")
                }

                val llvmGlobal = createLlvmGlobal(module!!, type, name, isExported)
                return Global(staticData, llvmGlobal)
            }
        }

        fun setInitializer(value: ConstValue) {
            LLVMSetInitializer(llvmGlobal, value.llvm)
        }

        fun setConstant(value: Boolean) {
            LLVMSetGlobalConstant(llvmGlobal, if (value) 1 else 0)
        }

        val pointer = Pointer.to(this)
    }

    /**
     * Represents the pointer to static data.
     * It can be a pointer to either a global or any its element.
     *
     * TODO: this class is probably should be implemented more optimally
     */
    class Pointer private constructor(val global: Global,
                                      private val delegate: ConstPointer,
                                      val offsetInGlobal: Long) : ConstPointer by delegate {

        companion object {
            fun to(global: Global) = Pointer(global, constPointer(global.llvmGlobal), 0L)
        }

        private fun getElementOffset(index: Int): Long {
            val llvmTargetData = global.staticData.llvmTargetData
            val type = LLVMGetElementType(delegate.llvmType)
            return when (LLVMGetTypeKind(type)) {
                LLVMTypeKind.LLVMStructTypeKind -> LLVMOffsetOfElement(llvmTargetData, type, index)
                LLVMTypeKind.LLVMArrayTypeKind -> LLVMABISizeOfType(llvmTargetData, LLVMGetElementType(type)) * index
                else -> TODO()
            }
        }

        override fun getElementPtr(index: Int): Pointer {
            return Pointer(global, delegate.getElementPtr(index), offsetInGlobal + this.getElementOffset(index))
        }

        /**
         * @return the distance from other pointer to this.
         *
         * @throws UnsupportedOperationException if it is not possible to represent the distance as [Int] value
         */
        fun sub(other: Pointer): Int {
            if (this.global != other.global) {
                throw UnsupportedOperationException("pointers must belong to the same global")
            }

            val res = this.offsetInGlobal - other.offsetInGlobal
            if (res.toInt().toLong() != res) {
                throw UnsupportedOperationException("result doesn't fit into Int")
            }

            return res.toInt()
        }
    }

    /**
     * Creates [Global] with given type and name.
     *
     * It is external until explicitly initialized with [Global.setInitializer].
     */
    fun createGlobal(type: LLVMTypeRef, name: String, isExported: Boolean = false): Global {
        return Global.create(this, type, name, isExported)
    }

    /**
     * Creates [Global] with given name and value.
     */
    fun placeGlobal(name: String, initializer: ConstValue, isExported: Boolean = false): Global {
        val global = createGlobal(initializer.llvmType, name, isExported)
        global.setInitializer(initializer)
        return global
    }

    /**
     * Creates array-typed global with given name and value.
     */
    fun placeGlobalArray(name: String, elemType: LLVMTypeRef?, elements: List<ConstValue>): Global {
        val initializer = ConstArray(elemType, elements)
        val global = placeGlobal(name, initializer)

        return global
    }

    private val stringLiterals = mutableMapOf<String, ConstPointer>()

    fun kotlinStringLiteral(type: KotlinType, value: IrConst<String>) =
        stringLiterals.getOrPut(value.value) { createKotlinStringLiteral(type, value) }
}
