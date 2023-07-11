// MODULE: m1-common
// FILE: common.kt

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    final override fun toString() = "Foo"
}
