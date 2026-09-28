// TARGET_BACKEND: JVM
// FULL_JDK

package test

public class Foo(val a: Int = 1, val b: Int) {}

fun box(): String {
    try {
        Class.forName("test.Foo").getDeclaredConstructor()
        return "Fail"
    } catch (e: NoSuchMethodException) {
        return "OK"
    }
}
