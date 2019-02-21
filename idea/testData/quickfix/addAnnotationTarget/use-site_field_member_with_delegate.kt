// "Add annotation target" "false"
// WITH_RUNTIME
// ACTION: Introduce import alias
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ERROR: '@field:' annotations could be applied only to properties with backing fields
// ERROR: This annotation is not applicable to target 'member property with delegate' and use site target '@field'

@Target
annotation class Ann

class Test {
    @field:Ann<caret>
    val baz: String by lazy { "" }
}