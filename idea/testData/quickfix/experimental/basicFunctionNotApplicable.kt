// "Add '@MyExperimentalAPI' annotation to 'bar'" "false"
// COMPILER_ARGUMENTS: -Xuse-experimental=kotlin.Experimental
// WITH_RUNTIME
// ACTION: Add '@MyExperimentalAPI' annotation to containing class 'Bar'
// ACTION: Add '@OptIn(MyExperimentalAPI::class)' annotation to 'bar'
// ACTION: Add '-Xuse-experimental=MyExperimentalAPI' to module light_idea_test_case compiler arguments
// ERROR: This declaration is experimental and its usage must be marked with '@MyExperimentalAPI' or '@OptIn(MyExperimentalAPI::class)'
// ERROR: This declaration is experimental and its usage must be marked with '@MyExperimentalAPI' or '@OptIn(MyExperimentalAPI::class)'

@Experimental
@Target(AnnotationTarget.CLASS)
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
