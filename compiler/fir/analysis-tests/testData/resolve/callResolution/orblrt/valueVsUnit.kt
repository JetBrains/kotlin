// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-49401

@JvmName("create1Value")
fun create1(code: () -> String): String = ""
fun create1(code: () -> Unit) {}

fun create2(code: () -> Unit) {}
@JvmName("create2Value")
fun create2(code: () -> String): String = ""

@JvmName("create3Value")
fun <S> create3(code: () -> S): S = TODO()
fun create3(code: () -> Unit) {}

fun create4(code: () -> Unit) {}
@JvmName("create4Value")
fun <S> create4(code: () -> S): S = TODO()

fun main() {
    val x11 = create1 { "a" }
    val x12 = create1 { println(123) }

    val x21 = create2 { "a" }
    val x22 = create2 { println(123) }

    val x31 = create3 { "a" }
    val x32 = create3 { println(123) }

    val x41 = create4 { "a" }
    val x42 = create4 { println(123) }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x11<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>x12<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x21<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>x22<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x31<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>x32<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x41<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>x42<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
