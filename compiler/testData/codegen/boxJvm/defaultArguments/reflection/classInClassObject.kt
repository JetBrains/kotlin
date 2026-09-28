// TARGET_BACKEND: JVM

package test

class A {
    companion object {
        class Foo(val a: Int = 1) {}
    }
}

fun box(): String {
    Class.forName("test.A\$Companion\$Foo").getDeclaredConstructor()
    return "OK"
}
