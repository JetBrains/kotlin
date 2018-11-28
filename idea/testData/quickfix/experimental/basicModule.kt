// "Add '-Xuse-experimental=test.MyExperimentalAPI' to module light_idea_test_case compiler arguments" "true"
// COMPILER_ARGUMENTS: -Xuse-experimental=kotlin.Experimental
// COMPILER_ARGUMENTS_AFTER: -Xuse-experimental=kotlin.Experimental -Xuse-experimental=test.MyExperimentalAPI
// DISABLE-ERRORS
// WITH_RUNTIME

package test

@Experimental
annotation class MyExperimentalAPI

@MyExperimentalAPI
class Some {
    fun foo() {}
}

class Bar {
    fun bar() {
        Some().foo<caret>()
    }
}