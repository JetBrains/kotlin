// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface MyInterface

var value: Any? = null

fun saveValue(a: Any?) {
    value = a
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class MyClass(private val value: Int): MyInterface {
    fun foo(other: MyInterface) {
        saveValue((other as? MyClass)?.value)
    }
}

fun box(): String {
    val x = MyClass(5)
    x.foo(x)
    if (value != 5) return "FAIL: $value"
    return "OK"
}