// False-positive reports in K1, because fixed only in K2
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    class Inner<T>
}

expect fun substituted<!NO_ACTUAL_FOR_EXPECT{JVM}!>(p: Foo.<!UNRESOLVED_REFERENCE{JVM}!>Inner<!><Any>)<!>
expect fun substitutedIncorrect<!NO_ACTUAL_FOR_EXPECT{JVM}!>(p: Foo.<!UNRESOLVED_REFERENCE{JVM}!>Inner<!><Any>)<!>

expect fun <T> withTypeParam<!NO_ACTUAL_FOR_EXPECT{JVM}!>(p: Foo.<!UNRESOLVED_REFERENCE{JVM}!>Inner<!><T>)<!>
expect fun <T, R> withTypeParamIncorrect<!NO_ACTUAL_FOR_EXPECT{JVM}!>(p: Foo.<!UNRESOLVED_REFERENCE{JVM}!>Inner<!><R>)<!>

expect fun star<!NO_ACTUAL_FOR_EXPECT{JVM}!>(p: Foo.<!UNRESOLVED_REFERENCE{JVM}!>Inner<!><*>)<!>
expect fun starVsNonStar<!NO_ACTUAL_FOR_EXPECT{JVM}!>(p: Foo.<!UNRESOLVED_REFERENCE{JVM}!>Inner<!><*>)<!>


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl {
    class Inner<T>
}

actual typealias Foo = FooImpl

actual fun substituted<!ACTUAL_WITHOUT_EXPECT!>(p: Foo.<!UNRESOLVED_REFERENCE!>Inner<!><Any>)<!> {}
actual fun substitutedIncorrect<!ACTUAL_WITHOUT_EXPECT!>(p: Foo.<!UNRESOLVED_REFERENCE!>Inner<!><String>)<!> {}

actual fun <T> withTypeParam<!ACTUAL_WITHOUT_EXPECT!>(p: Foo.<!UNRESOLVED_REFERENCE!>Inner<!><T>)<!> {}
actual fun <T, R> withTypeParamIncorrect<!ACTUAL_WITHOUT_EXPECT!>(p: Foo.<!UNRESOLVED_REFERENCE!>Inner<!><T>)<!> {}

actual fun star<!ACTUAL_WITHOUT_EXPECT!>(p: Foo.<!UNRESOLVED_REFERENCE!>Inner<!><*>)<!> {}
actual fun starVsNonStar<!ACTUAL_WITHOUT_EXPECT!>(p: Foo.<!UNRESOLVED_REFERENCE!>Inner<!><Any>)<!> {}

