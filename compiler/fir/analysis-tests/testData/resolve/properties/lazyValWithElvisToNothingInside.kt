// WITH_STDLIB
// ISSUE: KT-54668

interface A {
    val list: List<String>
}

fun getA(): A? = null

val x by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, NEW_INFERENCE_ERROR!>lazy {
    (getA() ?: error("error")).list.associateBy {
        it
    }
}<!>
