// MODULE: m1-common
// FILE: common.kt

import kotlin.reflect.KClass

expect annotation class Ann(
    val p: KClass<*> = Foo.Nested::class
)

expect class Foo {
    class Nested
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

import kotlin.reflect.KClass

class FooImpl {
    class Nested
}

actual typealias Foo = FooImpl

actual annotation class Ann(
    actual val p: KClass<*> = FooImpl.Nested::class
)
