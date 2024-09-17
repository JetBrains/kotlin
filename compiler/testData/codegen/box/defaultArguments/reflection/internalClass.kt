// TARGET_BACKEND: JVM_IR

package test

class Foo(val a: Int = 1) {}

fun box(): String {
    Class.forName("test.Foo").getDeclaredConstructor()
    return "OK"
}
