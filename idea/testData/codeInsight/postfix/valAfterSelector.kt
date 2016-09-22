class A {
    fun bar() = 1
}
fun foo(a: A) {
    val x = a.bar().val<caret>
}
