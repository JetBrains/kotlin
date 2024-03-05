class A(val property: Int)

fun foo() {
    val a = A(property = 10)
    println(a.pr<caret>operty)
}