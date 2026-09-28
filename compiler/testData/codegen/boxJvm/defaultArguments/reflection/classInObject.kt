// TARGET_BACKEND: JVM

package test

object o {
    class Foo(val a: Int = 1) {}
}

fun box(): String {
    Class.forName("test.o\$Foo").getDeclaredConstructor()
    return "OK"
}
