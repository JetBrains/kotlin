// WITH_STDLIB
// TARGET_BACKEND: JVM
// ISSUE: KT-61443

fun foo() = DelegatedB()()

interface A {
    operator fun invoke() {}

    operator fun invoke(value: String) = bar(value)
}

fun A.bar(value: String) {}

open class DelegatedB : B<String> by C()

interface B<out T> : A

class C<out T> : B<T>
