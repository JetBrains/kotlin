// TARGET_BACKEND: JVM_IR

package test

public class Foo(val a: Int = 1) {}

fun box(): String {
    Class.forName("test.Foo").getDeclaredConstructor()
    return "OK"
}
