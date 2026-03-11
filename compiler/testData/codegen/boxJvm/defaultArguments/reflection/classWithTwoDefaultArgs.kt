// TARGET_BACKEND: JVM_IR

package test

class Foo(val a: Int = 1, val b: String = "b") {}

fun box(): String {
    Class.forName("test.Foo").getDeclaredConstructor()
    return "OK"
}
