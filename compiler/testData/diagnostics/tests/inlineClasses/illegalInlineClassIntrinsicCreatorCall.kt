// FIR_IDENTICAL
// WITH_STDLIB
// LANGUAGE: +CustomBoxingInInlineClasses

@JvmInline
value class IC1<T>(val x: T)

@JvmInline
value class IC2(val x: Int)

@JvmInline
value class IC3<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>

inline fun <reified T> bar() = createInlineClassInstance<<!INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT!>T<!>>(5)

fun <T> bar2() = createInlineClassInstance<<!INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT, TYPE_PARAMETER_AS_REIFIED!>T<!>>(null)

fun func(x: IC1<String>) {}

fun foo() {
    createInlineClassInstance<<!INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT!>String<!>>("abacaba")
    createInlineClassInstance<<!INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT!>IC1<Int><!>>(3)
    createInlineClassInstance<IC2>(<!INTRINSIC_BOXING_CALL_ARGUMENT_TYPE_MISMATCH!>5.3<!>)
    createInlineClassInstance<IC2>(1)
    val x : IC1<Int> = <!INTRINSIC_BOXING_CALL_BAD_INFERRED_TYPE_ARGUMENT!>createInlineClassInstance(1)<!>
    val y : IC2 = createInlineClassInstance(1)
    val z : IC2 = createInlineClassInstance(<!INTRINSIC_BOXING_CALL_ARGUMENT_TYPE_MISMATCH!>"str"<!>)
    func(<!INTRINSIC_BOXING_CALL_BAD_INFERRED_TYPE_ARGUMENT!>createInlineClassInstance("aba")<!>)
    createInlineClassInstance<IC3>(0)
    <!INAPPLICABLE_CANDIDATE!>createInlineClassInstance<!><<!CANNOT_INFER_PARAMETER_TYPE!>IC2<!>, IC2>(1)
}