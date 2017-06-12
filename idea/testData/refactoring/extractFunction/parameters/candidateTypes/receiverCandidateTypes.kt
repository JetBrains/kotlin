// PARAM_DESCRIPTOR: public fun Bar.foo(): kotlin.Unit defined in root package in file receiverCandidateTypes.kt
// PARAM_TYPES: Bar, Foo, kotlin.Any
// WITH_RUNTIME

open class Foo
class Bar : Foo()

fun Bar.foo() {
    <selection>toString()</selection>
}