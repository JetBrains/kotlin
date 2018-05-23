// "Add '@MyExperimentalAPI' annotation to containing class 'Outer'" "false"
// COMPILER_ARGUMENTS: -Xuse-experimental=kotlin.Experimental
// WITH_RUNTIME
// ACTION: Add '@MyExperimentalAPI' annotation to 'bar'
// ACTION: Add '@MyExperimentalAPI' annotation to containing class 'Inner'
// ACTION: Add '@UseExperimental(MyExperimentalAPI::class)' annotation to 'bar'
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