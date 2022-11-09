// FIR_IDENTICAL
// WITH_STDLIB
// LANGUAGE: +CustomBoxingInInlineClasses

@JvmInline
value class IC1<T>(val x: T)

@JvmInline
value class IC2(val x: Int)

@JvmInline
value class IC3<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>

inline fun <reified T> bar() = boxByDefault<<!INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT!>T<!>>(5)

fun <T> bar2() = boxByDefault<<!INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT, TYPE_PARAMETER_AS_REIFIED!>T<!>>(null)

fun func(x: IC1<String>) {}

fun foo() {
    boxByDefault<<!INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT!>String<!>>("abacaba")
    boxByDefault<<!INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT!>IC1<Int><!>>(3)
    boxByDefault<IC2>(<!INTRINSIC_BOXING_CALL_ARGUMENT_TYPE_MISMATCH!>5.3<!>)
    boxByDefault<IC2>(1)
    val x : IC1<Int> = <!INTRINSIC_BOXING_CALL_BAD_INFERRED_TYPE_ARGUMENT!>boxByDefault(1)<!>
    val y : IC2 = boxByDefault(1)
    val z : IC2 = boxByDefault(<!INTRINSIC_BOXING_CALL_ARGUMENT_TYPE_MISMATCH!>"str"<!>)
    func(<!INTRINSIC_BOXING_CALL_BAD_INFERRED_TYPE_ARGUMENT!>boxByDefault("aba")<!>)
    boxByDefault<IC3>(0)
}