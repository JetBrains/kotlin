// ISSUE: KT-42130

interface A
interface B : A

fun B.foo(): Boolean = true
fun <T> run(action: () -> T): T = action()

fun foo(a: A): String {
    return when (a) {
        is B -> when {
            run { a.foo() } -> "OK"
            else -> "Fail 1"
        }
        else -> "Fail 2"
    }
}

class C : B

fun box(): String = foo(C())
