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

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.backend.konan.isValueType
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.isExported
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.backend.konan.llvm.symbolName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinOperatorDescriptorBase
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes

internal object DataFlowIR {

    abstract class Type {
        // Special marker type forbidding devirtualization on its instances.
        object Virtual : Declared(false, true, null, -1)

        class External(val hash: Long, val name: String? = null) : Type() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is External) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "ExternalType(hash='$hash', name='$name')"
            }
        }

        abstract class Declared(val isFinal: Boolean, val isAbstract: Boolean, val module: Module?, val symbolTableIndex: Int) : Type() {
            val superTypes = mutableListOf<Type>()
            val vtable = mutableListOf<FunctionSymbol>()
            val itable = mutableMapOf<Long, FunctionSymbol>()
        }

        class Public(val hash: Long, isFinal: Boolean, isAbstract: Boolean, module: Module, symbolTableIndex: Int, val name: String? = null)
            : Declared(isFinal, isAbstract, module, symbolTableIndex) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Public) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "PublicType(hash='$hash', symbolTableIndex='$symbolTableIndex', name='$name')"
            }
        }

        class Private(val index: Int, isFinal: Boolean, isAbstract: Boolean, module: Module, symbolTableIndex: Int, val name: String? = null)
            : Declared(isFinal, isAbstract, module, symbolTableIndex) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Private) return false

                return index == other.index
            }

            override fun hashCode(): Int {
                return index
            }

            override fun toString(): String {
                return "PrivateType(index=$index, symbolTableIndex='$symbolTableIndex', name='$name')"
            }
        }
    }

    class Module(val descriptor: ModuleDescriptor) {
        var numberOfFunctions = 0
        var numberOfClasses = 0
    }

    abstract class FunctionSymbol(val numberOfParameters: Int, val isGlobalInitializer: Boolean, val name: String?) {
        class External(val hash: Long, numberOfParameters: Int, isGlobalInitializer: Boolean,
                       val escapes: Int?, val pointsTo: IntArray?, name: String? = null)
            : FunctionSymbol(numberOfParameters, isGlobalInitializer, name) {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is External) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "ExternalFunction(hash='$hash', name='$name', escapes='$escapes', pointsTo='${pointsTo?.contentToString()}')"
            }
        }

        abstract class Declared(numberOfParameters: Int, val module: Module, val symbolTableIndex: Int,
                                isGlobalInitializer: Boolean, name: String?)
            : FunctionSymbol(numberOfParameters, isGlobalInitializer, name)

        class Public(val hash: Long, numberOfParameters: Int, module: Module, symbolTableIndex: Int,
                     isGlobalInitializer: Boolean, name: String? = null)
            : Declared(numberOfParameters, module, symbolTableIndex, isGlobalInitializer, name) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Public) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "PublicFunction(hash='$hash', symbolTableIndex='$symbolTableIndex', name='$name')"
            }
        }

        class Private(val index: Int, numberOfParameters: Int, module: Module, symbolTableIndex: Int,
                      isGlobalInitializer: Boolean, name: String? = null)
            : Declared(numberOfParameters, module, symbolTableIndex, isGlobalInitializer, name) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Private) return false

                return index == other.index
            }

            override fun hashCode(): Int {
                return index
            }

            override fun toString(): String {
                return "PrivateFunction(index=$index, symbolTableIndex='$symbolTableIndex', name='$name')"
            }
        }
    }

    data class Field(val type: Type?, val hash: Long, val name: String? = null)

    class Edge(val castToType: Type?) {

        lateinit var node: Node

        constructor(node: Node, castToType: Type?) : this(castToType) {
            this.node = node
        }
    }

    enum class VariableKind {
        Ordinary,
        Temporary,
        CatchParameter
    }

    sealed class Node {
        class Parameter(val index: Int) : Node()

        class Const(val type: Type) : Node()

        open class Call(val callee: FunctionSymbol, val arguments: List<Edge>, val returnType: Type,
                        open val callSite: IrFunctionAccessExpression?) : Node()

        class StaticCall(callee: FunctionSymbol, arguments: List<Edge>, returnType: Type,
                         val receiverType: Type?, callSite: IrFunctionAccessExpression?)
            : Call(callee, arguments, returnType, callSite)

        class NewObject(constructor: FunctionSymbol, arguments: List<Edge>, type: Type, override val callSite: IrCall?)
            : Call(constructor, arguments, type, callSite)

        open class VirtualCall(callee: FunctionSymbol, arguments: List<Edge>, returnType: Type,
                                   val receiverType: Type, override val callSite: IrCall?)
            : Call(callee, arguments, returnType, callSite)

        class VtableCall(callee: FunctionSymbol, receiverType: Type, val calleeVtableIndex: Int,
                         arguments: List<Edge>, returnType: Type, callSite: IrCall?)
            : VirtualCall(callee, arguments, returnType, receiverType, callSite)

        class ItableCall(callee: FunctionSymbol, receiverType: Type, val calleeHash: Long,
                         arguments: List<Edge>, returnType: Type, callSite: IrCall?)
            : VirtualCall(callee, arguments, returnType, receiverType, callSite)

        class Singleton(val type: Type, val constructor: FunctionSymbol?) : Node()

        class FieldRead(val receiver: Edge?, val field: Field) : Node()

        class FieldWrite(val receiver: Edge?, val field: Field, val value: Edge) : Node()

        class ArrayRead(val array: Edge, val index: Edge, val callSite: IrCall?) : Node()

        class ArrayWrite(val array: Edge, val index: Edge, val value: Edge) : Node()

        class Variable(values: List<Edge>, val type: Type, val kind: VariableKind) : Node() {
            val values = mutableListOf<Edge>().also { it += values }
        }
    }

    class FunctionBody(val nodes: List<Node>, val returns: Node.Variable, val throws: Node.Variable)

    class Function(val symbol: FunctionSymbol,
                   val parameterTypes: Array<Type>,
                   val returnType: Type,
                   val body: FunctionBody) {

        fun debugOutput() {
            println("FUNCTION $symbol")
            println("Params: ${parameterTypes.contentToString()}")
            val ids = body.nodes.withIndex().associateBy({ it.value }, { it.index })
            body.nodes.forEach {
                println("    NODE #${ids[it]!!}")
                printNode(it, ids)
            }
            println("    RETURNS")
            printNode(body.returns, ids)
        }

        companion object {
            fun printNode(node: Node, ids: Map<Node, Int>) = print(nodeToString(node, ids))

            fun nodeToString(node: Node, ids: Map<Node, Int>) = when (node) {
                is Node.Const ->
                    "        CONST ${node.type}\n"

                is Node.Parameter ->
                    "        PARAM ${node.index}\n"

                is Node.Singleton ->
                    "        SINGLETON ${node.type}\n"

                is Node.StaticCall -> {
                    val result = StringBuilder()
                    result.appendln("        STATIC CALL ${node.callee}")
                    node.arguments.forEach {
                        result.append("            ARG #${ids[it.node]!!}")
                        if (it.castToType == null)
                            result.appendln()
                        else
                            result.appendln(" CASTED TO ${it.castToType}")
                    }
                    result.toString()
                }

                is Node.VtableCall -> {
                    val result = StringBuilder()
                    result.appendln("        VIRTUAL CALL ${node.callee}")
                    result.appendln("            RECEIVER: ${node.receiverType}")
                    result.appendln("            VTABLE INDEX: ${node.calleeVtableIndex}")
                    node.arguments.forEach {
                        result.append("            ARG #${ids[it.node]!!}")
                        if (it.castToType == null)
                            result.appendln()
                        else
                            result.appendln(" CASTED TO ${it.castToType}")
                    }
                    result.toString()
                }

                is Node.ItableCall -> {
                    val result = StringBuilder()
                    result.appendln("        INTERFACE CALL ${node.callee}")
                    result.appendln("            RECEIVER: ${node.receiverType}")
                    result.appendln("            METHOD HASH: ${node.calleeHash}")
                    node.arguments.forEach {
                        result.append("            ARG #${ids[it.node]!!}")
                        if (it.castToType == null)
                            result.appendln()
                        else
                            result.appendln(" CASTED TO ${it.castToType}")
                    }
                    result.toString()
                }

                is Node.NewObject -> {
                    val result = StringBuilder()
                    result.appendln("        NEW OBJECT ${node.callee}")
                    result.appendln("        TYPE ${node.returnType}")
                    node.arguments.forEach {
                        result.append("            ARG #${ids[it.node]!!}")
                        if (it.castToType == null)
                            result.appendln()
                        else
                            result.appendln(" CASTED TO ${it.castToType}")
                    }
                    result.toString()
                }

                is Node.FieldRead -> {
                    val result = StringBuilder()
                    result.appendln("        FIELD READ ${node.field}")
                    result.append("            RECEIVER #${node.receiver?.node?.let { ids[it]!! } ?: "null"}")
                    if (node.receiver?.castToType == null)
                        result.appendln()
                    else
                        result.appendln(" CASTED TO ${node.receiver.castToType}")
                    result.toString()
                }

                is Node.FieldWrite -> {
                    val result = StringBuilder()
                    result.appendln("        FIELD WRITE ${node.field}")
                    result.append("            RECEIVER #${node.receiver?.node?.let { ids[it]!! } ?: "null"}")
                    if (node.receiver?.castToType == null)
                        result.appendln()
                    else
                        result.appendln(" CASTED TO ${node.receiver.castToType}")
                    print("            VALUE #${ids[node.value.node]!!}")
                    if (node.value.castToType == null)
                        result.appendln()
                    else
                        result.appendln(" CASTED TO ${node.value.castToType}")
                    result.toString()
                }

                is Node.ArrayRead -> {
                    val result = StringBuilder()
                    result.appendln("        ARRAY READ")
                    result.append("            ARRAY #${ids[node.array.node]}")
                    if (node.array.castToType == null)
                        result.appendln()
                    else
                        result.appendln(" CASTED TO ${node.array.castToType}")
                    result.append("            INDEX #${ids[node.index.node]!!}")
                    if (node.index.castToType == null)
                        result.appendln()
                    else
                        result.appendln(" CASTED TO ${node.index.castToType}")
                    result.toString()
                }

                is Node.ArrayWrite -> {
                    val result = StringBuilder()
                    result.appendln("        ARRAY WRITE")
                    result.append("            ARRAY #${ids[node.array.node]}")
                    if (node.array.castToType == null)
                        result.appendln()
                    else
                        result.appendln(" CASTED TO ${node.array.castToType}")
                    result.append("            INDEX #${ids[node.index.node]!!}")
                    if (node.index.castToType == null)
                        result.appendln()
                    else
                        result.appendln(" CASTED TO ${node.index.castToType}")
                    print("            VALUE #${ids[node.value.node]!!}")
                    if (node.value.castToType == null)
                        result.appendln()
                    else
                        result.appendln(" CASTED TO ${node.value.castToType}")
                    result.toString()
                }

                is Node.Variable -> {
                    val result = StringBuilder()
                    result.appendln("       ${node.kind}")
                    node.values.forEach {
                        result.append("            VAL #${ids[it.node]!!}")
                        if (it.castToType == null)
                            result.appendln()
                        else
                            result.appendln(" CASTED TO ${it.castToType}")
                    }
                    result.toString()
                }

                else -> {
                    "        UNKNOWN: ${node::class.java}\n"
                }
            }
        }
    }

    class SymbolTable(val context: Context, val irModule: IrModuleFragment, val module: Module) {

        private val TAKE_NAMES = false // Take fqNames for all functions and types (for debug purposes).

        private inline fun takeName(block: () -> String) = if (TAKE_NAMES) block() else null

        val classMap = mutableMapOf<ClassDescriptor, Type>()
        val functionMap = mutableMapOf<CallableDescriptor, FunctionSymbol>()

        private val NAME_ESCAPES = Name.identifier("Escapes")
        private val NAME_POINTS_TO = Name.identifier("PointsTo")
        private val FQ_NAME_KONAN = FqName.fromSegments(listOf("konan"))

        private val FQ_NAME_ESCAPES = FQ_NAME_KONAN.child(NAME_ESCAPES)
        private val FQ_NAME_POINTS_TO = FQ_NAME_KONAN.child(NAME_POINTS_TO)

        private val konanPackage = context.builtIns.builtInsModule.getPackage(FQ_NAME_KONAN).memberScope
        private val escapesAnnotationDescriptor = konanPackage.getContributedClassifier(
                NAME_ESCAPES, NoLookupLocation.FROM_BACKEND) as ClassDescriptor
        private val escapesWhoDescriptor = escapesAnnotationDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters.single()
        private val pointsToAnnotationDescriptor = konanPackage.getContributedClassifier(
                NAME_POINTS_TO, NoLookupLocation.FROM_BACKEND) as ClassDescriptor
        private val pointsToOnWhomDescriptor = pointsToAnnotationDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters.single()

        var privateTypeIndex = 0
        var privateFunIndex = 0

        init {
            irModule.accept(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    declaration.body?.let { mapFunction(declaration.descriptor) }
                }

                override fun visitField(declaration: IrField) {
                    declaration.initializer?.let { mapFunction(declaration.descriptor) }
                }

                override fun visitClass(declaration: IrClass) {
                    declaration.acceptChildrenVoid(this)

                    mapClass(declaration.descriptor)
                }
            }, data = null)
        }

        private fun ClassDescriptor.isFinal() = modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS

        fun mapClass(descriptor: ClassDescriptor): Type {
            // Do not try to devirtualize ObjC classes.
            if (descriptor.module.name == Name.special("<forward declarations>") || descriptor.isObjCClass())
                return Type.Virtual

            val name = descriptor.fqNameSafe.asString()
            if (descriptor.module != irModule.descriptor)
                return classMap.getOrPut(descriptor) { Type.External(name.localHash.value, takeName { name }) }

            classMap[descriptor]?.let { return it }

            val isFinal = descriptor.isFinal()
            val isAbstract = descriptor.isAbstract()
            val placeToClassTable = !descriptor.defaultType.isValueType()
            val symbolTableIndex = if (placeToClassTable) module.numberOfClasses++ else -1
            val type = if (descriptor.isExported())
                           Type.Public(name.localHash.value, isFinal, isAbstract, module, symbolTableIndex, takeName { name })
                       else
                           Type.Private(privateTypeIndex++, isFinal, isAbstract, module, symbolTableIndex, takeName { name })
            if (!descriptor.isInterface) {
                val vtableBuilder = context.getVtableBuilder(descriptor)
                type.vtable += vtableBuilder.vtableEntries.map { mapFunction(it.getImplementation(context)) }
                if (!isAbstract) {
                    vtableBuilder.methodTableEntries.forEach {
                        type.itable.put(
                                it.overriddenDescriptor.functionName.localHash.value,
                                mapFunction(it.getImplementation(context))
                        )
                    }
                }
            }
            classMap.put(descriptor, type)
            type.superTypes += descriptor.defaultType.immediateSupertypes().map { mapType(it) }
            return type
        }

        fun mapType(type: KotlinType) = mapClass(type.erasure().single())

        // TODO: use from LlvmDeclarations.
        private fun getFqName(descriptor: DeclarationDescriptor): FqName {
            if (descriptor is PackageFragmentDescriptor) {
                return descriptor.fqName
            }

            val containingDeclaration = descriptor.containingDeclaration
            val parent = if (containingDeclaration != null) {
                getFqName(containingDeclaration)
            } else {
                FqName.ROOT
            }

            val localName = descriptor.name
            return parent.child(localName)
        }

        private val FunctionDescriptor.internalName get() = getFqName(this).asString() + "#internal"

        fun mapFunction(descriptor: CallableDescriptor) = descriptor.original.let {
            functionMap.getOrPut(it) {
                when (it) {
                    is PropertyDescriptor -> {
                        // A global property initializer.
                        assert (it.dispatchReceiverParameter == null) { "All local properties initializers should've been lowered" }
                        FunctionSymbol.Private(privateFunIndex++, 0, module, -1, true, takeName { "${it.symbolName}_init" })
                    }

                    is FunctionDescriptor -> {
                        val name = if (it.isExported()) it.symbolName else it.internalName
                        val numberOfParameters = it.allParameters.size + if (it.isSuspend) 1 else 0
                        if (it.module != irModule.descriptor || it.isExternal || (it is IrBuiltinOperatorDescriptorBase)) {
                            val escapesAnnotation = it.annotations.findAnnotation(FQ_NAME_ESCAPES)
                            val pointsToAnnotation = it.annotations.findAnnotation(FQ_NAME_POINTS_TO)
                            @Suppress("UNCHECKED_CAST")
                            val escapesBitMask = (escapesAnnotation?.allValueArguments?.get(escapesWhoDescriptor.name) as? ConstantValue<Int>)?.value
                            @Suppress("UNCHECKED_CAST")
                            val pointsToBitMask = (pointsToAnnotation?.allValueArguments?.get(pointsToOnWhomDescriptor.name) as? ConstantValue<List<IntValue>>)?.value
                            FunctionSymbol.External(name.localHash.value, numberOfParameters, false, escapesBitMask,
                                    pointsToBitMask?.let { it.map { it.value }.toIntArray() }, takeName { name })
                        } else {
                            val isAbstract = it.modality == Modality.ABSTRACT
                            val classDescriptor = it.containingDeclaration as? ClassDescriptor
                            val placeToFunctionsTable = !isAbstract && it !is ConstructorDescriptor && classDescriptor != null
                                    && classDescriptor.kind != ClassKind.ANNOTATION_CLASS
                                    && (it.isOverridableOrOverrides || it.name.asString().contains("<bridge-") || !classDescriptor.isFinal())
                            val symbolTableIndex = if (placeToFunctionsTable) module.numberOfFunctions++ else -1
                            if (it.isExported())
                                FunctionSymbol.Public(name.localHash.value, numberOfParameters, module, symbolTableIndex, false, takeName { name })
                            else
                                FunctionSymbol.Private(privateFunIndex++, numberOfParameters, module, symbolTableIndex, false, takeName { name })
                        }
                    }

                    else -> error("Unknown descriptor: $it")
                }
            }
        }

        fun getPrivateFunctionsTableForExport() =
                functionMap
                        .asSequence()
                        .filter { it.key is FunctionDescriptor }
                        .filter { it.value.let { it is DataFlowIR.FunctionSymbol.Declared && it.symbolTableIndex >= 0 } }
                        .sortedBy { (it.value as DataFlowIR.FunctionSymbol.Declared).symbolTableIndex }
                        .apply {
                            forEachIndexed { index, entry ->
                                assert((entry.value  as DataFlowIR.FunctionSymbol.Declared).symbolTableIndex == index) { "Inconsistent function table" }
                            }
                        }
                        .map { it.key as FunctionDescriptor }
                        .toList()

        fun getPrivateClassesTableForExport() =
                classMap
                        .asSequence()
                        .filter { it.value.let { it is DataFlowIR.Type.Declared && it.symbolTableIndex >= 0 } }
                        .sortedBy { (it.value as DataFlowIR.Type.Declared).symbolTableIndex }
                        .apply {
                            forEachIndexed { index, entry ->
                                assert((entry.value  as DataFlowIR.Type.Declared).symbolTableIndex == index) { "Inconsistent class table" }
                            }
                        }
                        .map { it.key }
                        .toList()
    }
}