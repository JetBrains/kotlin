trait Foo {
}


[deprecated("boo")] fun bar() {}

enum class E {
    First Second
}

enum class F(val name: String) {
    First: F("First")
    Second: F("Second")
}

val f = { (a: Int, b: Int) -> a + b }

annotation class Ann(val arg1: Class<*>, val arg2: Class<out Any?>)

Ann(javaClass<String>(), javaClass<Int>()) class MyClass

class A private()

val x = fun foo(x: String) { }

fun foo() {
    @loop
    for (i in 1..100) {
        /* comment */
        continue@loop
    }
}
