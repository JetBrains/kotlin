// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-54668

interface A {
    val list: List<String>
}

fun getA(): A? = null

val x by lazy {
    (getA() ?: error("error")).list.associateBy {
        it
    }
}
