// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    class Inner {
        fun foo(p: List<Inner>)
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl {
    class Inner {
        fun foo(p: List<Inner>) {}
    }
}

actual typealias Foo = FooImpl
