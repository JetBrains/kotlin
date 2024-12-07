class A(val property: Int)

fun foo() {
    val a = A(pr<caret>operty = 10)
    println(a.property)
}