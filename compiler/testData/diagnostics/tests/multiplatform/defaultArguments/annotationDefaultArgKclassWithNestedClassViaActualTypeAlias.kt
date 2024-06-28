// MODULE: m1-common
// FILE: common.kt

import kotlin.reflect.KClass

expect annotation class Ann(
    val p: KClass<*> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT{JVM}!>Foo.<!UNRESOLVED_REFERENCE{JVM}!>Nested<!>::class<!>
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
    actual val p: KClass<*> = <!ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE!>FooImpl.Nested::class<!>
)
