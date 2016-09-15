package org.jetbrains.kotlin.native.interop.gen

open class CType
open class DirectlyMappedType(val kotlinType: String) : CType()
object VoidType : CType()
object Int8Type : DirectlyMappedType("Byte")
object UInt8Type : DirectlyMappedType("Byte")
object Int16Type : DirectlyMappedType("Short")
object UInt16Type : DirectlyMappedType("Short")
object Int32Type : DirectlyMappedType("Int")
object UInt32Type : DirectlyMappedType("Int")
object Int64Type : DirectlyMappedType("Long")
object UInt64Type : DirectlyMappedType("Long")
class RecordType(val name: String) : CType()
class EnumType(val name: String) : CType()
class PointerType(val pointeeType : CType) : CType()
class FunctionPointerType(val parameterTypes: List<CType>, val returnType: CType) : CType()
open class ArrayType(val elemType: CType) : CType()
class ConstArrayType(elemType: CType, val length: Int) : ArrayType(elemType)
class IncompleteArrayType(elemType: CType) : ArrayType(elemType)


fun parseType(type: String): CType = TypeParser(type).parse()

class TypeParser(private val type: String) {
    companion object {
        private val primitiveType = mapOf(
                "V"  to VoidType,
                "C"  to Int8Type,
                "UC" to UInt8Type,
                "UB" to UInt8Type,
                "S"  to Int16Type,
                "US" to UInt16Type,
                "I"  to Int32Type,
                "UI" to UInt32Type,
                "J"  to Int64Type,
                "UJ" to UInt64Type
        )
    }

    private var at = 0

    private fun at(s: String): Boolean = type.substring(at).startsWith(s)

    private fun expect(s: String) {
        if (!advance(s)) error("Expecting <$s> (at=$at)")
    }

    private fun advance(s: String): Boolean {
        if (at(s)) {
            at += s.length
            return true
        }
        return false
    }

    private fun error(s: String): Nothing = throw IllegalStateException(s + ": " + type)

    fun parse(): CType {
        if (at == type.length) error("No type to parse")

        @Suppress("UNUSED_VARIABLE")
        val isConst = advance("c")

        for ((string, type) in primitiveType.entries) {
            if (advance(string)) return type
        }

        if (advance("R")) {
            val semicolon = type.indexOf(';', at)
            if (semicolon < 0) error("L without a matching semicolon")
            val recordName = type.substring(at, semicolon)
            expect(recordName)
            expect(";")
            return RecordType(recordName)
        }

        if (advance("E")) { // TODO: copy-pasted!
            val semicolon = type.indexOf(';', at)
            if (semicolon < 0) error("E without a matching semicolon")
            val enumName = type.substring(at, semicolon)
            expect(enumName)
            expect(";")
            return EnumType(enumName)
        }

        if (advance("*(")) {
            val paramTypes = mutableListOf<CType>()
            while (!advance(")")) {
                if (advance(".")) {
                    // TODO: support vararg
                    continue
                }
                paramTypes.add(parse())
            }
            val returnType = parse()
            expect(";")
            return FunctionPointerType(paramTypes, returnType)
        }

        if (advance("*")) {
            val pointee = parse()
            expect(";")
            return PointerType(pointee)
        }

        if (advance("[:")) {
            val lengthEndIndex = type.indexOf(':', at)
            val length = Integer.parseInt(type.substring(at, lengthEndIndex))
            at = lengthEndIndex + 1
            val elemType = parse()
            expect(";")
            return ConstArrayType(elemType, length)
        }

        if (advance("[")) {
            val elemType = parse()
            expect(";")
            return IncompleteArrayType(elemType)
        }

        throw NotImplementedError("Unsupported type (at=$at): $type")
    }
}