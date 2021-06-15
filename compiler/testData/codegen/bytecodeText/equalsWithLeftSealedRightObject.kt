// TARGET_BACKEND: JVM
// WITH_RUNTIME

sealed class A
object B : A()
object C : A()

fun x(): A = B

fun test() {
    when (x()) {
        // must use eqeqeq
        B -> println(1)
        else -> println(2)
    }
}

// 1 IF_ACMPNE
