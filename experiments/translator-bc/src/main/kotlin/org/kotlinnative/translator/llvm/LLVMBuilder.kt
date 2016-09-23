package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.llvm.bc.BcLlvmBuilder
import org.kotlinnative.translator.llvm.ir.IrLlvmBuilder
import org.kotlinnative.translator.llvm.types.*
import java.rmi.UnexpectedException
import java.util.*

class LLVMBuilder(arm: Boolean = false) {
    private var variableCount = 0
    private var labelCount = 0

    val ir = IrLlvmBuilder()
    val bc = BcLlvmBuilder()

    fun addComment(comment: String) {
        ir.comment(comment)
        bc.comment(comment)
        //addLLVMCodeToLocalPlace("; " + comment)
    }

    fun addStartExpression() {
        ir.startExpression()
        bc.startExpression()
        //addLLVMCodeToLocalPlace("{")
    }

    fun addEndExpression() {
        ir.endExpression()
        bc.endExpression()
        //addLLVMCodeToLocalPlace("unreachable\n}")
    }

    fun addAssignment(lhs: LLVMVariable, rhs: LLVMNode) {
        ir.assignment(lhs, rhs)
        bc.assignment(lhs, rhs)
        //addLLVMCodeToLocalPlace("$lhs = $rhs")
    }

    fun addReturnOperator(llvmVariable: LLVMSingleValue) {
        ir.returnOperation(llvmVariable)
        bc.returnOperation(llvmVariable)
        //addLLVMCodeToLocalPlace("ret ${llvmVariable.type} $llvmVariable")
    }

    fun addAnyReturn(type: LLVMType, value: String = type.defaultValue, pointer: Int = 0) {
        ir.anyReturn(type, value, pointer)
        bc.anyReturn(type, value, pointer)
        //addLLVMCodeToLocalPlace("ret $type${"*".repeat(pointer)} $value")
    }

    fun addStringConstant(variable: LLVMVariable, value: String) {
        ir.stringConstant(variable, value)
        bc.stringConstant(variable, value)
        //addLLVMCodeToGlobalPlace("$variable = private unnamed_addr constant  ${(variable.type as LLVMStringType).fullArrayType} c\"${value.replace("\"", "\\\"")}\\00\", align 1")
    }

        fun addCondition(condition: LLVMSingleValue, thenLabel: LLVMLabel, elseLabel: LLVMLabel) {
            ir.conditionalBranch(condition, thenLabel, elseLabel)
            bc.conditionalBranch(condition, thenLabel, elseLabel)
            //addLLVMCodeToLocalPlace("br ${condition.pointedType} $condition, label $thenLabel, label $elseLabel")
        }

        fun addUnconditionalJump(label: LLVMLabel) {
            ir.unconditionalBranch(label)
            bc.unconditionalBranch(label)
            //addLLVMCodeToLocalPlace("br label $label")
        }

        fun addExceptionCall(exceptionName: String) {
            ir.abort(exceptionName)
            bc.abort(exceptionName)
            /*
            val exception = exceptions[exceptionName]
            val printResult = getNewVariable(LLVMIntType(), pointer = 0)
            addLLVMCodeToLocalPlace("$printResult = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds (${(exception!!.type as LLVMStringType).fullArrayType}* $exception, i32 0, i32 0))")
            addFunctionCall(LLVMVariable("abort", LLVMVoidType(), scope = LLVMVariableScope()), emptyList())
            */
        }

        fun addFunctionCall(functionName: LLVMVariable, arguments: List<LLVMVariable>) {
            ir.call(functionName, arguments)
            bc.call(functionName, arguments)
            //addLLVMCodeToLocalPlace("call ${functionName.type} $functionName(${arguments.joinToString { it -> "${it.type} $it" }})")
        }

        fun allocStackVar(target: LLVMVariable, asValue: Boolean = false, pointer: Boolean = false) {
            ir.alloca(target, asValue, pointer)
            bc.alloca(target, asValue, pointer)
            /*
            val type = if (asValue) target.type.toString() else target.pointedType
            addLLVMCodeToLocalPlace("$target = alloca ${if (pointer) type.removeSuffix("*") else type}, align ${target.type.align}")
            */
        }

        fun allocStaticVar(target: LLVMVariable, asValue: Boolean = false, pointer: Boolean = false) {
            ir.heapAndCast(target, asValue, pointer)
            bc.heapAndCall(target, asValue, pointer)
            /*
            val allocated = getNewVariable(LLVMCharType(), pointer = 1)
            val size = if ((target.pointer >= 2) || (target.pointer >= 1 && !pointer)) TranslationState.POINTER_SIZE else target.type.size

            addLLVMCodeToLocalPlace("$allocated = call i8* @malloc_heap(i32 $size)")
            addLLVMCodeToLocalPlace("$target = bitcast ${allocated.pointedType} $allocated to ${if (asValue) target.type.toString() else target.pointedType}" + if (pointer) "" else "*")
            */
        }

        fun addGlobalInitialize(target: LLVMVariable, fields: ArrayList<LLVMClassVariable>, initializers: Map<LLVMVariable, String>, classType: LLVMType) {
            ir.globalinitialize(target, fields, initializers, classType)
            bc.globalInitialize(target, fields, initializers, classType)
            /*
            val code = "$target = internal global $classType { ${
            fields.map { it.pointedType + " " + if (initializers.containsKey(it)) initializers[it] else "0" }.joinToString()
            } }, align ${classType.align}"
            addLLVMCodeToGlobalPlace(code)
            */
        }

        fun bitcast(src: LLVMVariable, llvmType: LLVMType, pointer: Int): LLVMVariable {
            ir.bitcast(src, llvmType, pointer)
            bc.bitcast(src, llvmType, pointer)
            /*
            val empty = getNewVariable(llvmType, pointer = pointer)
            addLLVMCodeToLocalPlace("$empty = bitcast ${src.pointedType} $src to ${llvmType.toString() + "*".repeat(pointer)}")
            return empty
            */
        }

        fun convertVariableToType(variable: LLVMSingleValue, targetType: LLVMType): LLVMSingleValue {
            var resultVariable = variable
            if (variable.type != targetType) {
                val convertedExpression = targetType.convertFrom(variable)
                resultVariable = saveExpression(convertedExpression)
            }
            return resultVariable
        }

        fun copyVariable(from: LLVMVariable, to: LLVMVariable) =
                when {
                    from.type is LLVMStringType && !from.type.isLoaded -> storeString(to, from, 0)
                    else -> copyVariableValue(to, from)
                }

        fun createClass(name: String, fields: List<LLVMVariable>) {
            ir.classDeclaration(name, fields)
            bc.classDeclaration(name, fields)
            //addLLVMCodeToGlobalPlace("%class.$name = type { ${fields.map { it.pointedType }.joinToString()} }")
        }

        fun declareEntryPoint(name: String) {
            TODO("not that it's required")
            /*
            addLLVMCodeToLocalPlace("define weak void @main()")
            addStartExpression()
            addFunctionCall(LLVMVariable(name, LLVMVoidType(), scope = LLVMVariableScope()), listOf())
            addAnyReturn(LLVMVoidType())
            addEndExpression()
            */
        }

        fun defineGlobalVariable(variable: LLVMVariable, defaultValue: String = variable.type.defaultValue) {
            ir.globalDeclaration(variable, defaultValue)
            bc.globalDeclaration(variable, defaultValue)
            //addLLVMCodeToLocalPlace("$variable = global ${variable.pointedType} $defaultValue, align ${variable.type.align}")
        }

        fun getNewVariable(type: LLVMType, pointer: Int = 0, kotlinName: String? = null, scope: LLVMScope = LLVMRegisterScope(), prefix: String = "var"): LLVMVariable {
            variableCount++
            return LLVMVariable("$prefix$variableCount", type, kotlinName, scope, pointer)
        }

        fun getNewLabel(scope: LLVMScope = LLVMRegisterScope(), prefix: String): LLVMLabel {
            labelCount++
            return LLVMLabel("label.$prefix.$labelCount", scope)
        }

        fun loadArgsIfRequired(names: List<LLVMSingleValue>, args: List<LLVMVariable>) =
                names.mapIndexed(fun(i: Int, value: LLVMSingleValue): LLVMSingleValue {
                    return receivePointedArgument(value, args[i].pointer)
                }).toList()

        fun loadArgument(llvmVariable: LLVMVariable, store: Boolean = true): LLVMVariable {
            val allocVar = LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName, LLVMRegisterScope(), pointer = llvmVariable.pointer + 1)
            allocStackVar(allocVar, asValue = false, pointer = true)
            if (store) {
                storeVariable(allocVar, llvmVariable)
            }
            return allocVar
        }

        fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
            ir.loadClassField(target, source, offset)
            bc.loadClassField(target, source, offset)
            //addLLVMCodeToLocalPlace("$target = getelementptr inbounds ${source.pointedType} $source, i32 0, i32 $offset")
        }

        fun loadVariableOffset(target: LLVMVariable, source: LLVMVariable, index: LLVMConstant) {
            ir.loadVariableOffset(target, source, index)
            bc.loadVariableOffset(target, source, index)
            //addLLVMCodeToLocalPlace("$target = getelementptr inbounds ${source.type} $source, ${index.type} ${index.value}")
        }

        fun loadAndGetVariable(source: LLVMVariable): LLVMVariable {
            assert(source.pointer > 0)
            val target = getNewVariable(source.type, source.pointer - 1, source.kotlinName)
            ir.load(source)
            bc.load(source)
            //addLLVMCodeToLocalPlace("$target = load ${source.pointedType} $source, align ${target.type.align}")

            return target
        }

        fun makeStructInitializer(args: List<LLVMVariable>, values: List<String>) {
            ir.structInitializer(args, values)
            bc.structInitializer(args, values)
            // = "{ ${args.mapIndexed { i: Int, variable: LLVMVariable -> "${variable.type} ${values[i]}" }.joinToString()} }"
        }

        fun markWithLabel(label: LLVMLabel?) {
            label ?: return
            ir.label(label)
            bc.label(label)
            //addLLVMCodeToLocalPlace("${label.label}:")
        }

        fun memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int = 4, volatile: Boolean = false) {
            //addLLVMCodeToLocalPlace("call void @llvm.memcpy.p0i8.p0i8.i64(i8* $castedDst, i8* $castedSrc, i64 $size, i32 $align, i1 $volatile)")
            ir.memcpy(castedDst, castedSrc, size, align, volatile)
            bc.memcpy(castedDst, castedSrc, size, align, volatile)
        }

        fun nullCheck(variable: LLVMVariable): LLVMVariable {
            val result = getNewVariable(LLVMBooleanType(), pointer = 0)
            val loaded = loadAndGetVariable(variable)
            addLLVMCodeToLocalPlace("$result = icmp eq ${loaded.pointedType} null, $loaded")
            return result
        }

        fun receiveNativeValue(firstOp: LLVMSingleValue): LLVMSingleValue =
                when {
                    firstOp is LLVMConstant || firstOp.pointer == 0 -> firstOp
                    firstOp is LLVMVariable -> loadAndGetVariable(firstOp)
                    else -> throw UnexpectedException("Unknown inheritor of LLVMSingleValue")
                }

        fun receivePointedArgument(value: LLVMSingleValue, requirePointer: Int): LLVMSingleValue {
            var result = value

            while (result.pointer > requirePointer) {
                result = receiveNativeValue(result)
            }

            if ((value.type is LLVMStringType) && !(value.type.isLoaded)) {
                val newVariable = getNewVariable(value.type, pointer = result.pointer + 1)
                allocStackVar(newVariable, asValue = true)
                copyVariable(result as LLVMVariable, newVariable)
                result = loadAndGetVariable(newVariable)
            }

            return result
        }

        fun storeString(target: LLVMVariable, source: LLVMVariable, offset: Int) {
            val code = "store ${target.type} getelementptr inbounds (" +
                    "${(source.type as LLVMStringType).fullArrayType}* $source, i32 0, i32 $offset), ${target.pointedType} $target, align ${source.type.align}"
            (target.type as LLVMStringType).isLoaded = true
            addLLVMCodeToLocalPlace(code)
        }

        fun storeVariable(target: LLVMSingleValue, source: LLVMSingleValue) {
            if ((source.type is LLVMStringType) && !(source.type.isLoaded)) {
                storeString(target as LLVMVariable, source as LLVMVariable, 0)
            } else {
                addLLVMCodeToLocalPlace("store ${source.pointedType} $source, ${target.pointedType} $target, align ${source.type.align}")
            }
        }

        fun saveExpression(expression: LLVMSingleValue): LLVMVariable {
            val resultOp = getNewVariable(expression.type, pointer = expression.pointer)
            addAssignment(resultOp, expression)
            return resultOp
        }

        fun storeNull(result: LLVMVariable) =
                addLLVMCodeToLocalPlace("store ${result.pointedType.dropLast(1)} null, ${result.pointedType} $result, align ${TranslationState.POINTER_ALIGN}")

        override fun toString() = globalCode.toString() + localCode.toString()

        private fun copyVariableValue(target: LLVMVariable, source: LLVMVariable) {
            var from = source
            if (source.pointer > 0) {
                from = getNewVariable(source.type, source.pointer)
                addLLVMCodeToLocalPlace("$from = load ${source.pointedType} $source, align ${from.type.align}")
            }
            addLLVMCodeToLocalPlace("store ${target.type} $from, ${target.pointedType} $target, align ${from.type.align}")
        }
    }
}