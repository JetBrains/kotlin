// MODULE: m1-common
// FILE: common.kt

interface I

open class Base {
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun foo(): I = null!!<!>
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo<T : I> : Base {
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo<T : I> : Base() {
    override fun foo(): T = null!!
}
