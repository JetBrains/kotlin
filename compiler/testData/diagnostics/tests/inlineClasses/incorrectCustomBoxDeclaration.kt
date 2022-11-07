// FIR_IDENTICAL
// WITH_STDLIB
// !DIAGNOSTICS: -EXTENSION_SHADOWED_BY_MEMBER
// LANGUAGE: +CustomBoxingInInlineClasses

@JvmInline
value class IC1(val x: Int) {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun box(y: Int) = IC1(y)
}

class A(val x: Int) {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun box(y: Int) = A(y)
    }
}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun IC1.box(x: Int) = IC1(x)

@JvmInline
value class IC2(val x: Int) {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun box() = IC2(0)
    }
}

@JvmInline
value class IC3(val x: Int) {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun box(y: Int, z: Int) = IC2(0)
    }
}

@JvmInline
value class IC4(val x: Int) {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun box(y: String) = IC2(0)
    }
}

@JvmInline
value class IC5<T>(val x: T) {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun <T> box(y: T) = IC5(y)
    }
}

@JvmInline
value class IC6(val x: Int) {
    companion object MyCompanion {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun box(y: Number) = IC6(y.toInt())
    }
}

@JvmInline
value class IC7(val x: String) {
    companion object {
        operator fun box(y: String): Nothing = TODO()
    }
}

@JvmInline
value class IC8(val x: Int) {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun box(y : Int = 5) = IC8(y)
    }
}