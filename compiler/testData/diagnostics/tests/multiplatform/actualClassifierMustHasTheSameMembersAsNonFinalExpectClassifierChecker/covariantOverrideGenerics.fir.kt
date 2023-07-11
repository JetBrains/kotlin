// MODULE: m1-common
// FILE: common.kt

open class Base<R> {
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun foo(): R = null!!<!>
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo<R, T : R> : Base<R> {
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo<E, F : E> : Base<E>() {
    override fun foo(): F = null!!
}
