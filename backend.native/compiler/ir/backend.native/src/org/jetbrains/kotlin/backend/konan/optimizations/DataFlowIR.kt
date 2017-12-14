package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.externalOrIntrinsic
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.isExported
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.backend.konan.llvm.symbolName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes

internal object DataFlowIR {

    abstract class Type {
        // Special marker type forbidding devirtualization on its instances.
        object Virtual : Declared(false, true)

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

        abstract class Declared(val isFinal: Boolean, val isAbstract: Boolean) : Type() {
            val superTypes = mutableListOf<Type>()
            val vtable = mutableListOf<FunctionSymbol>()
            val itable = mutableMapOf<Long, FunctionSymbol>()
        }

        class Public(val hash: Long, isFinal: Boolean, isAbstract: Boolean, val name: String? = null) : Declared(isFinal, isAbstract) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Public) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "PublicType(hash='$hash', name='$name')"
            }
        }

        class Private(val index: Int, isFinal: Boolean, isAbstract: Boolean, val name: String? = null) : Declared(isFinal, isAbstract) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Private) return false

                return index == other.index
            }

            override fun hashCode(): Int {
                return index
            }

            override fun toString(): String {
                return "PrivateType(index=$index, name='$name')"
            }
        }
    }

    class Module(val descriptor: ModuleDescriptor) {
        var numberOfFunctions = 0
    }

    abstract class FunctionSymbol(val numberOfParameters: Int, val name: String?) {
        class External(val hash: Long, numberOfParameters: Int, name: String? = null)
            : FunctionSymbol(numberOfParameters, name) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is External) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "ExternalFunction(hash='$hash', name='$name')"
            }
        }

        abstract class Declared(numberOfParameters: Int, val module: Module, val symbolTableIndex: Int, name: String?)
            : FunctionSymbol(numberOfParameters, name)

        class Public(val hash: Long, numberOfParameters: Int, module: Module, symbolTableIndex: Int, name: String? = null)
            : Declared(numberOfParameters, module, symbolTableIndex, name) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Public) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "PublicFunction(hash='$hash', name='$name')"
            }
        }

        class Private(val index: Int, numberOfParameters: Int, module: Module, symbolTableIndex: Int, name: String? = null)
            : Declared(numberOfParameters, module, symbolTableIndex, name) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Private) return false

                return index == other.index
            }

            override fun hashCode(): Int {
                return index
            }

            override fun toString(): String {
                return "PrivateFunction(index=$index, name='$name')"
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

        class Variable(values: List<Edge>, val temp: Boolean) : Node() {
            val values = mutableListOf<Edge>().also { it += values }
        }
    }

    class FunctionBody(val nodes: List<Node>, val returns: Node.Variable, val throws: Node.Variable)

    class Function(val symbol: FunctionSymbol,
                   val isGlobalInitializer: Boolean,
                   val numberOfParameters: Int,
                   val body: FunctionBody) {


        fun printNode(node: Node, ids: Map<Node, Int>) {
            when (node) {
                is Node.Const ->
                    println("        CONST ${node.type}")

                is Node.Parameter ->
                    println("        PARAM ${node.index}")

                is Node.Singleton ->
                    println("        SINGLETON ${node.type}")

                is Node.StaticCall -> {
                    println("        STATIC CALL ${node.callee}")
                    node.arguments.forEach {
                        print("            ARG #${ids[it.node]!!}")
                        if (it.castToType == null)
                            println()
                        else
                            println(" CASTED TO ${it.castToType}")
                    }
                }

                is Node.VtableCall -> {
                    println("        VIRTUAL CALL ${node.callee}")
                    println("            RECEIVER: ${node.receiverType}")
                    println("            VTABLE INDEX: ${node.calleeVtableIndex}")
                    node.arguments.forEach {
                        print("            ARG #${ids[it.node]!!}")
                        if (it.castToType == null)
                            println()
                        else
                            println(" CASTED TO ${it.castToType}")
                    }
                }

                is Node.ItableCall -> {
                    println("        INTERFACE CALL ${node.callee}")
                    println("            RECEIVER: ${node.receiverType}")
                    println("            METHOD HASH: ${node.calleeHash}")
                    node.arguments.forEach {
                        print("            ARG #${ids[it.node]!!}")
                        if (it.castToType == null)
                            println()
                        else
                            println(" CASTED TO ${it.castToType}")
                    }
                }

                is Node.NewObject -> {
                    println("        NEW OBJECT ${node.callee}")
                    println("        TYPE ${node.returnType}")
                    node.arguments.forEach {
                        print("            ARG #${ids[it.node]!!}")
                        if (it.castToType == null)
                            println()
                        else
                            println(" CASTED TO ${it.castToType}")
                    }
                }

                is Node.FieldRead -> {
                    println("        FIELD READ ${node.field}")
                    print("            RECEIVER #${node.receiver?.node?.let { ids[it]!! } ?: "null"}")
                    if (node.receiver?.castToType == null)
                        println()
                    else
                        println(" CASTED TO ${node.receiver.castToType}")
                }

                is Node.FieldWrite -> {
                    println("        FIELD WRITE ${node.field}")
                    print("            RECEIVER #${node.receiver?.node?.let { ids[it]!! } ?: "null"}")
                    if (node.receiver?.castToType == null)
                        println()
                    else
                        println(" CASTED TO ${node.receiver.castToType}")
                    print("            VALUE #${ids[node.value.node]!!}")
                    if (node.value.castToType == null)
                        println()
                    else
                        println(" CASTED TO ${node.value.castToType}")
                }

                is Node.Variable -> {
                    println("        ${if (node.temp) "TEMP VAR" else "VARIABLE"} ")
                    node.values.forEach {
                        print("            VAL #${ids[it.node]!!}")
                        if (it.castToType == null)
                            println()
                        else
                            println(" CASTED TO ${it.castToType}")
                    }

                }

                else -> {
                    println("        UNKNOWN: ${node::class.java}")
                }
            }
        }

        fun debugOutput() {
            println("FUNCTION TEMPLATE $symbol")
            println("Params: $numberOfParameters")
            val ids = body.nodes.withIndex().associateBy({ it.value }, { it.index })
            body.nodes.forEach {
                println("    NODE #${ids[it]!!}")
                printNode(it, ids)
            }
            println("    RETURNS")
            printNode(body.returns, ids)
        }
    }

    class SymbolTable(val context: Context, val irModule: IrModuleFragment, val module: Module) {

        private val TAKE_NAMES = false // Take fqNames for all functions and types (for debug purposes).

        private inline fun takeName(block: () -> String) = if (TAKE_NAMES) block() else null

        val classMap = mutableMapOf<ClassDescriptor, Type>()
        val functionMap = mutableMapOf<CallableDescriptor, FunctionSymbol>()

        var privateTypeIndex = 0
        var privateFunIndex = 0
        var couldBeCalledVirtuallyIndex = 0

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
            val type = if (descriptor.isExported())
                Type.Public(name.localHash.value, isFinal, isAbstract, takeName { name })
            else
                Type.Private(privateTypeIndex++, isFinal, isAbstract, takeName { name })
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

        fun mapType(type: KotlinType) =
                mapClass(type.erasure().single().constructor.declarationDescriptor as ClassDescriptor)

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
                    is PropertyDescriptor ->
                        FunctionSymbol.Private(privateFunIndex++, 0, module, -1, takeName { "${it.symbolName}_init" })

                    is FunctionDescriptor -> {
                        val name = if (it.isExported()) it.symbolName else it.internalName
                        val numberOfParameters = it.allParameters.size + if (it.isSuspend) 1 else 0
                        if (it.module != irModule.descriptor || it.externalOrIntrinsic())
                            FunctionSymbol.External(name.localHash.value, numberOfParameters, takeName { name })
                        else {
                            val isAbstract = it.modality == Modality.ABSTRACT
                            val classDescriptor = it.containingDeclaration as? ClassDescriptor
                            val placeToFunctionsTable = !isAbstract && it !is ConstructorDescriptor && classDescriptor != null
                                    && classDescriptor.kind != ClassKind.ANNOTATION_CLASS
                                    && (it.isOverridableOrOverrides || it.name.asString().contains("<bridge-") || !classDescriptor.isFinal())
                            if (placeToFunctionsTable)
                                ++module.numberOfFunctions
                            val symbolTableIndex = if (!placeToFunctionsTable) -1 else couldBeCalledVirtuallyIndex++
                            if (it.isExported())
                                FunctionSymbol.Public(name.localHash.value, numberOfParameters, module, symbolTableIndex, takeName { name })
                            else
                                FunctionSymbol.Private(privateFunIndex++, numberOfParameters, module, symbolTableIndex, takeName { name })
                        }
                    }

                    else -> error("Unknown descriptor: $it")
                }
            }
        }
    }
}