// "Add '-Xopt-in=test.MyExperimentalAPI' to module light_idea_test_case compiler arguments" "true"
// COMPILER_ARGUMENTS: -Xopt-in=kotlin.RequiresOptIn
// COMPILER_ARGUMENTS_AFTER: -Xopt-in=kotlin.RequiresOptIn -Xopt-in=test.MyExperimentalAPI
// DISABLE-ERRORS
// WITH_RUNTIME

package test

@RequiresOptIn
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
