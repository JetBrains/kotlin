// ISSUE: KT-79866
// IGNORE_BACKEND_K1: JVM_IR
// LANGUAGE: -ForbidUsingExpressionTypesWithInaccessibleContent
// MODULE: bar
// FILE: Bar.kt

package test

class Bar<T>

class Bar2

// MODULE: foo(bar)
// FILE: Foo.kt

package test

fun foo1(array: Array<Bar<*>>? = null) {}

fun foo2(array: Array<Bar2>? = null) {}

fun foo3(bar: Bar<*>? = null) {}

// MODULE: test(foo)
// FILE: Test.kt

package test

@Suppress("MISSING_DEPENDENCY_CLASS")
fun main() {
    foo1()
    foo2()
    foo3()
}

// 1 INVOKESTATIC test/FooKt.foo1\$default \(\[Lerror/NonExistentClass;ILjava/lang/Object;\)V
