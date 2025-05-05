// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-17417

interface A {
    fun foo(): Int

    val bar: String
}

object B : A by B
