package org.jetbrains.kotlin.backend.konan.cgen

internal interface CType {
    fun render(name: String): String
}

internal class CVariable(val type: CType, val name: String) {
    override fun toString() = type.render(name)
}

internal object CTypes {
    fun simple(type: String): CType = SimpleCType(type)
    fun pointer(pointee: CType): CType = PointerCType(pointee)
    fun function(returnType: CType, parameterTypes: List<CType>, variadic: Boolean): CType =
            FunctionCType(returnType, parameterTypes, variadic)

    fun blockPointer(pointee: CType): CType = object : CType {
        override fun render(name: String): String = pointee.render("^$name")
    }

    val void = simple("void")
    val voidPtr = pointer(void)
    val signedChar = simple("signed char")
    val unsignedChar = simple("unsigned char")
    val short = simple("short")
    val unsignedShort = simple("unsigned short")
    val int = simple("int")
    val unsignedInt = simple("unsigned int")
    val longLong = simple("long long")
    val unsignedLongLong = simple("unsigned long long")
    val float = simple("float")
    val double = simple("double")
    val C99Bool = simple("_Bool")
    val char = simple("char")

    val id = simple("id")
}

private class SimpleCType(private val type: String) : CType {
    override fun render(name: String): String = if (name.isEmpty()) type else "$type $name"
}

private class PointerCType(private val pointee: CType) : CType {
    override fun render(name: String): String = pointee.render("*$name")
}

private class FunctionCType(
        private val returnType: CType,
        private val parameterTypes: List<CType>,
        private val variadic: Boolean
) : CType {
    override fun render(name: String): String = returnType.render(buildString {
        append("(")
        append(name)
        append(")(")
        parameterTypes.joinTo(this) { it.render("") }
        if (parameterTypes.isEmpty()) {
            if (!variadic) append("void")
        } else {
            if (variadic) append(", ...")
        }
        append(')')
    })
}