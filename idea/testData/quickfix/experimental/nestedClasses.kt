// "Add '@MyExperimentalAPI' annotation to containing class 'Outer'" "false"
// COMPILER_ARGUMENTS: -Xuse-experimental=kotlin.Experimental
// WITH_RUNTIME
// ACTION: Add '@MyExperimentalAPI' annotation to 'bar'
// ACTION: Add '@MyExperimentalAPI' annotation to containing class 'Inner'
// ACTION: Add '@UseExperimental(MyExperimentalAPI::class)' annotation to 'bar'
// ACTION: Add '-Xuse-experimental=MyExperimentalAPI' to module light_idea_test_case compiler arguments
// ERROR: This declaration is experimental and its usage must be marked with '@MyExperimentalAPI' or '@UseExperimental(MyExperimentalAPI::class)'

@Experimental
annotation class MyExperimentalAPI

@MyExperimentalAPI
fun foo() {}

class Outer {
    class Bar {
        class Inner {
            fun bar() {
                foo<caret>()
            }
        }
    }
}