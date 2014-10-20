// "Create function 'set' from usage" "true"
class A {
    fun get(s: String): Int = 1
}

fun foo() {
    var a = A()
    a<caret>["1"]++
}
