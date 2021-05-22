// "Replace with safe (?.) call" "true"
class A {
    operator fun plusAssign(other: A) {}
}

fun foo(b: A) {
    var a: A? = A()
    a <caret>+= b
}
