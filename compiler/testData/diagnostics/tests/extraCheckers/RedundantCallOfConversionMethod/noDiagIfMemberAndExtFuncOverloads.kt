// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-80060
// DIAGNOSTICS: -UNUSED_VARIABLE

class C {
    fun testIntLong(x: Long) = "call from member (Int, Long ambiguity)"
    fun testUIntULong(x: ULong) = "call from member (UInt, ULong ambiguity)"
}

fun C.testIntLong(x: Int) = "call from extension (Int, Long ambiguity)"
fun C.testUIntULong(x: UInt) = "call from extension (UInt, ULong ambiguity)"

fun <T> select(left: T, right: T) = right

fun test() {
    val c = C()
    c.testIntLong(1.toInt()) // The `toInt` call forces resolving to the extension, the REDUNDANT_CALL_OF_CONVERSION_METHOD shouldn't be reported here
    c.testIntLong((2 + 3).toInt()) // Resolving to the extension, no REDUNDANT_CALL_OF_CONVERSION_METHOD
    c.testIntLong(((2 + 3) * 4).toInt()) // Resolving to the extension, no REDUNDANT_CALL_OF_CONVERSION_METHOD
    c.testIntLong('d'.<!DEPRECATION!>toInt<!>()) // Dispatch receiver is not `Int` -> always non-redundant, resolving to the member
    c.testIntLong(4) // The integer literal type is treated as Long here and it forces the call to be resolved to the member
    c.testIntLong(5L) // Long literal, no ambiguity, resolving to the member

    c.testUIntULong(6U.toUInt()) // Resolving to the extension
    c.testUIntULong((7U + 8U).toUInt()) // Resolving to the extension
    c.testUIntULong(9U) // Resolving to the member
    c.testUIntULong(10UL) // Resolving to the member

    val e = select(17.toShort(), 16).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toShort()<!> // Redundant, the type of `select` is unambiguous after resolving
    val f = select(18.toInt(), 19).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>  // Redundant, the type of `select` is unambiguous after resolving
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
