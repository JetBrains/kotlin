// MODULE: m1-common
// FILE: common.kt

open class Base<T> {
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun foo(t: T) {}<!>
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo : Base<String><!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base<String>() {
    final override fun foo(t: String) {}
}
