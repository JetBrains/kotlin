// TARGET_BACKEND: JVM
// FILE: Caller.java

class Caller {
    public static void invoke(K<C, B, A> x) {
        x.f(null);
    }
}

// FILE: test.kt

open class A
open class B : A()
class C : B()

class K<T, U, V>() where T : U, U : V?, V : Any {
    fun f(x: T) { x!! }
}

fun box() =
    try {
        Caller.invoke(K<C, B, A>())
        "fail"
    } catch (e: NullPointerException) {
        "OK"
    }
