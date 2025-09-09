// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValues

fun stringF(): String = ""
fun nsf(): String? = "null"

fun Any.consume(): Unit = Unit

inline fun <T, R> T.myLet(block: (T) -> R): R {
    return block(this)
}

fun returnsString(): String {
    nsf()?.myLet { return it } // inferred to Nothing
    return ""
}

fun main() {
    stringF().<!RETURN_VALUE_NOT_USED!>myLet<!> { it }
    stringF().<!RETURN_VALUE_NOT_USED!>myLet<!> { 2 }
    stringF().let { 2 }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, integerLiteral, lambdaLiteral, nullableType, safeCall, stringLiteral, thisExpression, typeParameter */
