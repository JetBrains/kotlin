// COMPILATION_ERRORS
annotation class Ann0
interface I
class Foo {
    fun foo() {
        val i = object : I {
            @<caret>Ann0 @Suppress
        }
    }
}