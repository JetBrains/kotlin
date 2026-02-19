// COMPILATION_ERRORS
annotation class Ann0
class Foo {
    fun foo() {
        class Local {
            @<caret>Ann0 @Suppress
        }
    }
}