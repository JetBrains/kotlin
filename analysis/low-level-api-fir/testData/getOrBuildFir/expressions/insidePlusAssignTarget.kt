// WITH_STDLIB
// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtCallExpression

fun foo(x: MutableMap<Int, MutableList<String>>) {
    x.getOrPut(1) { <expr>mutableListOf<String>()</expr> } += "str"
}