package org.jetbrains.kotlin.native.interop.indexer

import java.io.File

fun buildNativeIndex(headerFile: File, args: List<String>): NativeIndex = buildNativeIndexImpl(headerFile, args)

abstract class NativeIndex {
    abstract val structs: List<StructDecl>
    abstract val enums: List<EnumDef>
    abstract val functions: List<FunctionDecl>
}


class Field(val name: String, val type: Type, val offset: Long) {
    override fun toString(): String = "$type $name at $offset"
}

abstract class StructDecl(val spelling: String) {

    abstract val def: StructDef?

}

abstract class StructDef(val size: Long, val decl: StructDecl) {
    abstract val fields: List<Field>
}

class EnumValue(val name: String, val value: Long)

abstract class EnumDef(val spelling: String, val baseType: PrimitiveType) {
    abstract val values: List<EnumValue>
}

class Parameter(val name: String?, val type: Type)

class FunctionDecl(val name: String, val parameters: List<Parameter>, val returnType: Type)



open class Type

open class PrimitiveType : Type()

object VoidType : Type()

object Int8Type : PrimitiveType()
object UInt8Type : PrimitiveType()

object Int16Type : PrimitiveType()
object UInt16Type : PrimitiveType()

object Int32Type : PrimitiveType()
object UInt32Type : PrimitiveType()

object IntPtrType : PrimitiveType()
object UIntPtrType : PrimitiveType()

object Int64Type : PrimitiveType()
object UInt64Type : PrimitiveType()

class RecordType(val decl: StructDecl) : Type()

class EnumType(val def: EnumDef) : Type()

class PointerType(val pointeeType : Type) : Type()

class FunctionType(val parameterTypes: List<Type>, val returnType: Type) : Type()

open class ArrayType(val elemType: Type) : Type()
class ConstArrayType(elemType: Type, val length: Long) : ArrayType(elemType)
class IncompleteArrayType(elemType: Type) : ArrayType(elemType)

object UnsupportedType : Type()

