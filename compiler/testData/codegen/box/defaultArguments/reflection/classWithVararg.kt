// TARGET_BACKEND: JVM_IR

package test

class Foo(vararg val a: String = arrayOfNulls<String>(1) as Array<String>)

fun box(): String {
    Class.forName("test.Foo").getDeclaredConstructor()
    return "OK"
}
