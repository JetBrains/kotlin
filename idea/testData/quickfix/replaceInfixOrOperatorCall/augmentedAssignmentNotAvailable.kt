// "Replace with safe (?.) call" "true"
class A {
    operator fun plus(other: A) = this
}

fun foo(b: A) {
    var a: A? = A()
    a <caret>+= b
}

/* IGNORE_FIR */
