// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: A.java

public abstract class A<T> {
    protected abstract String doIt(T... args);

    class B<S extends T, U extends S> {
        public String test(T... args) {
            return doIt(args);
        }

        public String test2(S... args) {
            return doIt(args);
        }

        public String test3(U... args) {
            return doIt(args);
        }
    }
}

// MODULE: main(lib)
// FILE: 1.kt

open class Super
open class Sub: Super()
class Sub2: Sub()

val a: A<Super> =
    object : A<Super>() {
        override fun doIt(vararg parameters: Super): String = "OK"
    }

fun box(): String {
    val b = a.B<Sub, Sub2>()
    if (b.test() != "OK") return "FAIL1"
    if (b.test2() != "OK") return "FAIL2"
    return b.test3()
}
