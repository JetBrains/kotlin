// False-positive reports in K1, because fixed only in K2
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    class Inner<T>
}

expect fun substituted(p: Foo.Inner<Any>)
<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect fun substitutedIncorrect(p: Foo.Inner<Any>)<!>

expect fun <T> withTypeParam(p: Foo.Inner<T>)
<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect fun <T, R> withTypeParamIncorrect(p: Foo.Inner<R>)<!>

expect fun star(p: Foo.Inner<*>)
<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect fun starVsNonStar(p: Foo.Inner<*>)<!>


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl {
    class Inner<T>
}

actual typealias Foo = FooImpl

actual fun substituted(p: Foo.Inner<Any>) {}
actual fun <!ACTUAL_WITHOUT_EXPECT!>substitutedIncorrect<!>(p: Foo.Inner<String>) {}

actual fun <T> withTypeParam(p: Foo.Inner<T>) {}
actual fun <T, R> <!ACTUAL_WITHOUT_EXPECT!>withTypeParamIncorrect<!>(p: Foo.Inner<T>) {}

actual fun star(p: Foo.Inner<*>) {}
actual fun <!ACTUAL_WITHOUT_EXPECT!>starVsNonStar<!>(p: Foo.Inner<Any>) {}

