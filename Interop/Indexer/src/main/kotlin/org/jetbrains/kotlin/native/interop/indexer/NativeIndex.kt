package org.jetbrains.kotlin.native.interop.indexer

enum class Language {
    C
}

class NativeLibrary(val includes: List<String>, val compilerArgs: List<String>, val language: Language)

/**
 * Retrieves the definitions from given C header file using given compiler arguments (e.g. defines).
 */
fun buildNativeIndex(library: NativeLibrary): NativeIndex = buildNativeIndexImpl(library)

/**
 * This class describes the IR of definitions from C header file(s).
 */
abstract class NativeIndex {
    abstract val structs: List<StructDecl>
    abstract val enums: List<EnumDef>
    abstract val typedefs: List<TypedefDef>
    abstract val functions: List<FunctionDecl>
    abstract val macroConstants: List<ConstantDef>
}

/**
 * C struct field.
 */
class Field(val name: String, val type: Type, val offset: Long)

/**
 * C struct declaration.
 */
abstract class StructDecl(val spelling: String) {

    abstract val def: StructDef?
}

/**
 * C struct definition.
 *
 * @param hasNaturalLayout must be `false` if the struct has unnatural layout, e.g. it is `packed`.
 * May be `false` even if the struct has natural layout.
 */
abstract class StructDef(val size: Long, val align: Int,
                         val decl: StructDecl,
                         val hasNaturalLayout: Boolean) {

    abstract val fields: List<Field>
}

/**
 * C enum value.
 */
class EnumValue(val name: String, val value: Long)

/**
 * C enum definition.
 */
abstract class EnumDef(val spelling: String, val baseType: PrimitiveType) {

    abstract val values: List<EnumValue>
}

/**
 * C function parameter.
 */
class Parameter(val name: String?, val type: Type)

/**
 * C function declaration.
 */
class FunctionDecl(val name: String, val parameters: List<Parameter>, val returnType: Type)

/**
 * C typedef definition.
 *
 * ```
 * typedef $aliased $name;
 * ```
 */
class TypedefDef(val aliased: Type, val name: String)

abstract class ConstantDef(val name: String, val type: Type)
class IntegerConstantDef(name: String, type: Type, val value: Long) : ConstantDef(name, type)
class FloatingConstantDef(name: String, type: Type, val value: Double) : ConstantDef(name, type)


/**
 * C type.
 */
interface Type

interface PrimitiveType : Type

object VoidType : Type

object Int8Type : PrimitiveType
object UInt8Type : PrimitiveType

object Int16Type : PrimitiveType
object UInt16Type : PrimitiveType

object Int32Type : PrimitiveType
object UInt32Type : PrimitiveType

object IntPtrType : PrimitiveType
object UIntPtrType : PrimitiveType

object Int64Type : PrimitiveType
object UInt64Type : PrimitiveType

object Float32Type : PrimitiveType
object Float64Type : PrimitiveType

data class RecordType(val decl: StructDecl) : Type

data class EnumType(val def: EnumDef) : Type

data class PointerType(val pointeeType : Type) : Type

data class FunctionType(val parameterTypes: List<Type>, val returnType: Type) : Type

interface ArrayType : Type {
    val elemType: Type
}

data class ConstArrayType(override val elemType: Type, val length: Long) : ArrayType
data class IncompleteArrayType(override val elemType: Type) : ArrayType

data class Typedef(val def: TypedefDef) : Type

object UnsupportedType : Type