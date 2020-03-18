// ISSUE: KT-37327

interface Q

sealed class A : Q

class B(val x: Int) : A()

fun Q.foo() {
    if (this !is A) return
    when (this) {
        is B -> x // unresolved
    }
}