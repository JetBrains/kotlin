/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.isMultiFieldValueClassType
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name

typealias TypeArguments = Map<IrTypeParameterSymbol, IrType>

sealed interface MfvcNode {
    val type: IrType
    val leavesCount: Int

    fun createInstanceFromBox(
        scope: IrBlockBuilder,
        typeArguments: TypeArguments,
        receiver: IrExpression?,
        accessType: AccessType,
        saveVariable: (IrVariable) -> Unit,
    ): ReceiverBasedMfvcNodeInstance
}

fun MfvcNode.createInstanceFromBox(
    scope: IrBlockBuilder,
    receiver: IrExpression,
    accessType: AccessType,
    saveVariable: (IrVariable) -> Unit
) =
    createInstanceFromBox(scope, makeTypeArgumentsFromType(receiver.type as IrSimpleType), receiver, accessType, saveVariable)

fun MfvcNode.createInstanceFromValueDeclarationsAndBoxType(
    scope: IrBuilderWithScope, type: IrSimpleType, name: Name, saveVariable: (IrVariable) -> Unit,
): ValueDeclarationMfvcNodeInstance = createInstanceFromValueDeclarations(scope, makeTypeArgumentsFromType(type), name, saveVariable)

fun MfvcNode.createInstanceFromValueDeclarations(
    scope: IrBuilderWithScope, typeArguments: TypeArguments, name: Name, saveVariable: (IrVariable) -> Unit,
): ValueDeclarationMfvcNodeInstance {
    val valueDeclarations = mapLeaves {
        scope.savableStandaloneVariable(
            type = it.type,
            name = listOf(name, it.fullFieldName).joinToString("-"),
            origin = IrDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_REPRESENTATION_VARIABLE,
            saveVariable = saveVariable
        )
    }
    return ValueDeclarationMfvcNodeInstance(this, typeArguments, valueDeclarations)
}

fun MfvcNode.createInstanceFromValueDeclarationsAndBoxType(
    type: IrSimpleType, fieldValues: List<IrValueDeclaration>
): ValueDeclarationMfvcNodeInstance =
    ValueDeclarationMfvcNodeInstance(this, makeTypeArgumentsFromType(type), fieldValues)

fun makeTypeArgumentsFromType(type: IrSimpleType): TypeArguments {
    if (type.classifierOrNull !is IrClassSymbol) return mapOf()
    val parameters = type.erasedUpperBound.typeParameters
    val arguments = type.arguments
    require(parameters.size == arguments.size) {
        "Number of type parameters (${parameters.joinToString { it.render() }}) is not equal to number of type arguments (${arguments.joinToString { it.render() }})."
    }
    return parameters.zip(arguments) { parameter, argument -> parameter.symbol to (argument.typeOrNull ?: parameter.defaultType) }.toMap()

}

sealed interface NameableMfvcNode : MfvcNode {
    val namedNodeImpl: NameableMfvcNodeImpl
}

val NameableMfvcNode.nameParts: List<IndexedNamePart>
    get() = namedNodeImpl.nameParts
val NameableMfvcNode.name: Name
    get() = nameParts.last().name
val NameableMfvcNode.index: Int
    get() = nameParts.last().index
val NameableMfvcNode.unboxMethod: IrSimpleFunction
    get() = namedNodeImpl.unboxMethod
val NameableMfvcNode.fullMethodName: Name
    get() = namedNodeImpl.fullMethodName
val NameableMfvcNode.fullFieldName: Name
    get() = namedNodeImpl.fullFieldName
val NameableMfvcNode.hasPureUnboxMethod: Boolean
    get() = namedNodeImpl.hasPureUnboxMethod


data class IndexedNamePart(val index: Int, val name: Name)

class NameableMfvcNodeImpl(
    rootPropertyName: String?,
    val nameParts: List<IndexedNamePart>,
    val unboxMethod: IrSimpleFunction,
    val hasPureUnboxMethod: Boolean,
) {
    val fullMethodName = makeFullMethodName(rootPropertyName, nameParts)
    val fullFieldName = makeFullFieldName(rootPropertyName, nameParts)

    companion object {
        @JvmStatic
        fun makeFullMethodName(rootPropertyName: String?, nameParts: List<IndexedNamePart>): Name = if (rootPropertyName == null) {
            val restJoined = nameParts.joinToString("-") { it.index.toString() }
            Name.identifier("${KotlinTypeMapper.UNBOX_JVM_METHOD_NAME}-$restJoined")
        } else {
            val wholeName = (listOf(JvmAbi.getterName(rootPropertyName)) + nameParts.map { it.index.toString() }).joinToString("-")
            Name.identifier(wholeName)
        }

        @JvmStatic
        fun makeFullFieldName(rootPropertyName: String?, nameParts: List<IndexedNamePart>): Name {
            val joined = (listOf(rootPropertyName ?: "field") + nameParts.map { it.index.toString() }).joinToString("-")
            return Name.identifier(joined)
        }
    }
}

fun MfvcNode.getSubnodeAndIndices(name: Name): Pair<NameableMfvcNode, IntRange>? {
    val node = (this as? MfvcNodeWithSubnodes)?.get(name) ?: return null
    val indices = subnodeIndices[node] ?: error("existing node without indices")
    return node to indices
}

sealed interface MfvcNodeWithSubnodes : MfvcNode {
    abstract override val type: IrSimpleType
    val boxMethod: IrSimpleFunction
    val leavesUnboxMethods: List<IrSimpleFunction>?
    val subnodesImpl: MfvcNodeWithSubnodesImpl
}

fun MfvcNodeWithSubnodes.makeBoxedExpression(
    scope: IrBuilderWithScope, typeArguments: TypeArguments, valueArguments: List<IrExpression>
): IrExpression = scope.irCall(boxMethod).apply {
    val resultType = type.substitute(typeArguments) as IrSimpleType
    require(resultType.erasedUpperBound == type.erasedUpperBound) { "Substitution of $type led to $resultType" }
    for ((index, typeArgument) in resultType.arguments.withIndex()) {
        putTypeArgument(index, typeArgument.typeOrNull ?: resultType.erasedUpperBound.typeParameters[index].defaultType)
    }
    for ((index, valueArgument) in valueArguments.withIndex()) {
        putValueArgument(index, valueArgument)
    }
}

operator fun MfvcNodeWithSubnodes.get(names: List<Name>): MfvcNode? {
    var cur: MfvcNode = this
    for (name in names) {
        cur = (cur as? MfvcNodeWithSubnodes)?.get(name) ?: return null
    }
    return cur
}

private fun List<Any>.allEqual() = all { it == first() }

class MfvcNodeWithSubnodesImpl(val subnodes: List<NameableMfvcNode>, unboxMethod: IrSimpleFunction?) {
    init {
        require(subnodes.isNotEmpty())
        require(subnodes.map { it.nameParts.dropLast(1) }.allEqual())
        require(subnodes.map { it.index } == subnodes.indices.toList())
    }

    private val mapping = subnodes.associateBy { it.name }.also { mapping ->
        require(mapping.size == subnodes.size) {
            subnodes
                .groupBy { it.name }
                .filterValues { it.size > 1 }
                .entries.joinToString(prefix = "Repeating node names found: ") { (name, nodes) -> "${nodes.size} nodes with name '$name'" }
        }
    }

    operator fun get(name: Name): NameableMfvcNode? = mapping[name]
    val leaves: List<LeafMfvcNode> = mapLeaves { it }
    val fields: List<IrField>? = mapLeaves { it.field }.run {
        @Suppress("UNCHECKED_CAST")
        when {
            all { it == null } -> null
            all { it != null } -> this as List<IrField>
            else -> error("IrFields can either exist all or none for MFVC property")
        }
    }

    val allInnerUnboxMethods: List<IrSimpleFunction> = subnodes.flatMap { subnode ->
        when (subnode) {
            is MfvcNodeWithSubnodes -> subnode.allUnboxMethods
            is LeafMfvcNode -> listOf(subnode.unboxMethod)
        }
    }

    val allUnboxMethods = allInnerUnboxMethods + listOfNotNull(unboxMethod)

    val indices: IntRange = leaves.indices

    val subnodeIndices: Map<NameableMfvcNode, IntRange> = buildMap {
        var offset = 0
        for (node in subnodes) {
            when (node) {
                is IntermediateMfvcNode -> {
                    val nodeSize = node.leavesCount
                    put(node, offset until offset + nodeSize)
                    putAll(node.subnodeIndices.mapValues { (_, v) -> (v.first + offset)..(v.last + offset) })
                    offset += nodeSize
                }

                is LeafMfvcNode -> {
                    put(node, offset..offset)
                    offset++
                }
            }
        }
    }
}

val MfvcNodeWithSubnodes.subnodes: List<NameableMfvcNode>
    get() = subnodesImpl.subnodes

operator fun MfvcNodeWithSubnodes.get(name: Name): NameableMfvcNode? = subnodesImpl[name]
val MfvcNodeWithSubnodes.leaves: List<LeafMfvcNode>
    get() = subnodesImpl.leaves
val MfvcNodeWithSubnodes.fields: List<IrField>?
    get() = subnodesImpl.fields
val RootMfvcNode.fields: List<IrField>
    get() = subnodesImpl.fields!!
val MfvcNodeWithSubnodes.indices: IntRange
    get() = subnodesImpl.indices
val MfvcNodeWithSubnodes.subnodeIndices: Map<NameableMfvcNode, IntRange>
    get() = subnodesImpl.subnodeIndices
val MfvcNodeWithSubnodes.allUnboxMethods: List<IrSimpleFunction>
    get() = subnodesImpl.allUnboxMethods
val MfvcNodeWithSubnodes.allInnerUnboxMethods: List<IrSimpleFunction>
    get() = subnodesImpl.allInnerUnboxMethods

inline fun <R> MfvcNode.mapLeaves(crossinline f: (LeafMfvcNode) -> R): List<R> = flatMapLeaves { listOf(f(it)) }

fun <R> MfvcNode.flatMapLeaves(f: (LeafMfvcNode) -> List<R>): List<R> = when (this) {
    is MfvcNodeWithSubnodes -> subnodes.flatMap { it.flatMapLeaves(f) }
    is LeafMfvcNode -> f(this)
}

inline fun <R> MfvcNodeWithSubnodesImpl.mapLeaves(crossinline f: (LeafMfvcNode) -> R): List<R> = subnodes.flatMap { it.mapLeaves(f) }


private fun requireSameClasses(vararg classes: IrClass?) {
    val notNulls = classes.filterNotNull()
    require(notNulls.zipWithNext { a, b -> a == b }.all { it }) {
        "Found different classes: ${notNulls.joinToString("\n") { it.render() }}"
    }
}

private fun requireSameSizes(vararg sizes: Int) {
    require(sizes.asList().zipWithNext { a, b -> a == b }.all { it }) {
        "Found different sizes: ${sizes.joinToString()}"
    }
}

private fun validateGettingAccessorParameters(function: IrSimpleFunction) {
    require(function.valueParameters.isEmpty()) { "Value parameters are not expected for ${function.render()}" }
    require(function.extensionReceiverParameter == null) { "Extension receiver is not expected for ${function.render()}" }
    require(function.contextReceiverParametersCount == 0) { "Context receivers is not expected for ${function.render()}" }
    require(function.typeParameters.isEmpty()) { "Type parameters are not expected for ${function.render()}" }
}

class LeafMfvcNode(
    override val type: IrType,
    rootPropertyName: String?,
    nameParts: List<IndexedNamePart>,
    val field: IrField?,
    unboxMethod: IrSimpleFunction,
    hasPureUnboxMethod: Boolean,
) : NameableMfvcNode {
    override val namedNodeImpl: NameableMfvcNodeImpl = NameableMfvcNodeImpl(rootPropertyName, nameParts, unboxMethod, hasPureUnboxMethod)

    override val leavesCount: Int
        get() = 1

    init {
        requireSameClasses(
            field?.parentAsClass?.takeUnless { unboxMethod.parentAsClass.isCompanion },
            unboxMethod.parentAsClass,
            (unboxMethod.dispatchReceiverParameter?.type as? IrSimpleType)?.erasedUpperBound,
        )
        validateGettingAccessorParameters(unboxMethod)
    }

    override fun createInstanceFromBox(
        scope: IrBlockBuilder,
        typeArguments: TypeArguments,
        receiver: IrExpression?,
        accessType: AccessType,
        saveVariable: (IrVariable) -> Unit,
    ) = ReceiverBasedMfvcNodeInstance(
        scope, this, typeArguments, receiver, field?.let(::listOf), unboxMethod, accessType, saveVariable
    )

    override fun toString(): String = "$fullFieldName: ${type.render()}"
}

val MfvcNode.fields
    get() = when (this) {
        is MfvcNodeWithSubnodes -> this.fields
        is LeafMfvcNode -> field?.let(::listOf)
    }

class IntermediateMfvcNode(
    override val type: IrSimpleType,
    rootPropertyName: String?,
    nameParts: List<IndexedNamePart>,
    subnodes: List<NameableMfvcNode>,
    unboxMethod: IrSimpleFunction,
    hasPureUnboxMethod: Boolean,
    val rootNode: RootMfvcNode, // root node corresponding type of the node
) : NameableMfvcNode, MfvcNodeWithSubnodes {
    override val namedNodeImpl: NameableMfvcNodeImpl = NameableMfvcNodeImpl(rootPropertyName, nameParts, unboxMethod, hasPureUnboxMethod)
    override val subnodesImpl: MfvcNodeWithSubnodesImpl = MfvcNodeWithSubnodesImpl(subnodes, unboxMethod)
    override val leavesCount
        get() = leaves.size

    init {
        require(type.needsMfvcFlattening()) { "MFVC type expected but got% ${type.render()}" }
        require(type.erasedUpperBound == rootNode.mfvc) {
            "Root node must point at the RootNode of class ${type.erasedUpperBound.render()} but points at ${rootNode.mfvc.render()}"
        }
        requireSameClasses(
            unboxMethod.parentAsClass,
            (unboxMethod.dispatchReceiverParameter?.type as IrSimpleType?)?.erasedUpperBound,
        )
        validateGettingAccessorParameters(unboxMethod)
    }

    override val boxMethod: IrSimpleFunction
        get() = rootNode.boxMethod

    override val leavesUnboxMethods: List<IrSimpleFunction> = collectLeavesUnboxMethods()

    override fun createInstanceFromBox(
        scope: IrBlockBuilder,
        typeArguments: TypeArguments,
        receiver: IrExpression?,
        accessType: AccessType,
        saveVariable: (IrVariable) -> Unit,
    ) = ReceiverBasedMfvcNodeInstance(
        scope, this, typeArguments, receiver, fields, unboxMethod, accessType, saveVariable
    )

    override fun toString(): String =
        "$fullFieldName: ${type.render()}\n${subnodes.joinToString("\n").prependIndent("    ")}"
}

private fun MfvcNodeWithSubnodes.collectLeavesUnboxMethods() = mapLeaves { it.unboxMethod }

fun IrSimpleFunction.isDefaultGetter(expectedField: IrField? = null): Boolean {
    if (!isGetter) return false
    if (expectedField != null && correspondingPropertySymbol?.owner?.backingField != expectedField) return false
    val statement = (body?.statements?.singleOrNull() as? IrReturn)?.value as? IrGetField ?: return false
    val actualField = statement.symbol.owner
    return expectedField == null || actualField == expectedField || parentAsClass.isCompanion && actualField.correspondingPropertySymbol == correspondingPropertySymbol
}

fun IrSimpleFunction.getGetterField(): IrField? {
    if (!isGetter) return null
    val statement = (body?.statements?.singleOrNull() as? IrReturn)?.value as? IrGetField ?: return null
    return statement.symbol.owner
}

class RootMfvcNode internal constructor(
    val mfvc: IrClass,
    subnodes: List<NameableMfvcNode>,
    val oldPrimaryConstructor: IrConstructor,
    val newPrimaryConstructor: IrConstructor,
    val primaryConstructorImpl: IrSimpleFunction,
    override val boxMethod: IrSimpleFunction,
    val specializedEqualsMethod: IrSimpleFunction,
) : MfvcNodeWithSubnodes {
    override val subnodesImpl: MfvcNodeWithSubnodesImpl = MfvcNodeWithSubnodesImpl(subnodes, null)
    override val type: IrSimpleType = mfvc.defaultType

    override val leavesCount: Int
        get() = leaves.size

    override val leavesUnboxMethods: List<IrSimpleFunction> = collectLeavesUnboxMethods()

    init {
        require(type.needsMfvcFlattening()) { "MFVC type expected but got% ${type.render()}" }
        for (constructor in listOf(oldPrimaryConstructor, newPrimaryConstructor)) {
            require(constructor.isPrimary) { "Expected a primary constructor but got:\n${constructor.dump()}" }
        }
        requireSameClasses(
            mfvc,
            oldPrimaryConstructor.parentAsClass,
            newPrimaryConstructor.parentAsClass,
            primaryConstructorImpl.parentAsClass,
            boxMethod.parentAsClass,
            specializedEqualsMethod.parentAsClass,
            oldPrimaryConstructor.constructedClass,
            newPrimaryConstructor.constructedClass,
            boxMethod.returnType.erasedUpperBound,
        )
        require(primaryConstructorImpl.returnType.isUnit()) {
            "Constructor-impl must return Unit but returns ${primaryConstructorImpl.returnType.render()}"
        }
        require(specializedEqualsMethod.returnType.isBoolean()) {
            "Specialized equals method must return Boolean but returns ${primaryConstructorImpl.returnType.render()}"
        }
        require(oldPrimaryConstructor.typeParameters.isEmpty() && newPrimaryConstructor.typeParameters.isEmpty()) {
            "Constructors do not support type parameters yet"
        }
        requireSameSizes(
            mfvc.typeParameters.size,
            boxMethod.typeParameters.size,
            primaryConstructorImpl.typeParameters.size,
        )
        require(specializedEqualsMethod.typeParameters.size == 2 * mfvc.typeParameters.size) {
            "Specialized equals method must contain twice more type parameters than corresponding MFVC ${mfvc.typeParameters.map { it.defaultType.render() }} but has ${specializedEqualsMethod.typeParameters.map { it.defaultType.render() }}"
        }
        requireSameSizes(oldPrimaryConstructor.valueParameters.size, subnodes.size)
        requireSameSizes(
            leavesCount,
            newPrimaryConstructor.valueParameters.size,
            primaryConstructorImpl.valueParameters.size,
            boxMethod.valueParameters.size,
        )
        require(specializedEqualsMethod.valueParameters.size == 2 * leavesCount) {
            "Specialized equals method must contain twice more value parameters than corresponding primary constructor of the MFVC ${mfvc.typeParameters.map { it.defaultType.render() }} but has ${specializedEqualsMethod.typeParameters.map { it.defaultType.render() }}"
        }
        for (function in listOf(oldPrimaryConstructor, newPrimaryConstructor, primaryConstructorImpl, boxMethod, specializedEqualsMethod)) {
            require(function.extensionReceiverParameter == null) { "Extension receiver is not expected for ${function.render()}" }
            require(function.contextReceiverParametersCount == 0) { "Context receivers are not expected for ${function.render()}" }
        }
    }

    override fun createInstanceFromBox(
        scope: IrBlockBuilder,
        typeArguments: TypeArguments,
        receiver: IrExpression?,
        accessType: AccessType,
        saveVariable: (IrVariable) -> Unit,
    ) = ReceiverBasedMfvcNodeInstance(scope, this, typeArguments, receiver, fields, null, accessType, saveVariable)

    override fun toString(): String =
        "${type.render()}\n${subnodes.joinToString("\n").prependIndent("    ")}"
}

fun IrType.needsMfvcFlattening(): Boolean = isMultiFieldValueClassType() && !isNullable() ||
        classifierOrNull.let { classifier ->
            classifier is IrTypeParameterSymbol && classifier.owner.superTypes.any { it.needsMfvcFlattening() }
        } // add not is annotated as @UseBox etc
