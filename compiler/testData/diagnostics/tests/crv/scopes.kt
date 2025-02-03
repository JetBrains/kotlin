// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

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
    stringF().myLet { it }
    stringF().myLet { 2 }
    stringF().let { 2 }
}
