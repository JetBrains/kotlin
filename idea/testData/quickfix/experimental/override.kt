// "Add '@MyExperimentalAPI' annotation to containing class 'Derived'" "false"
// COMPILER_ARGUMENTS: -Xuse-experimental=kotlin.Experimental
// WITH_RUNTIME
// ACTION: Add '@MyExperimentalAPI' annotation to 'foo'
// ACTION: Add '@OptIn(MyExperimentalAPI::class)' annotation to 'foo'
// ACTION: Add '@OptIn(MyExperimentalAPI::class)' annotation to containing class 'Derived'
// ACTION: Add '-Xuse-experimental=MyExperimentalAPI' to module light_idea_test_case compiler arguments
// ERROR: This declaration overrides experimental member of supertype 'Base' and must be annotated with '@MyExperimentalAPI'

@Experimental
@Target(AnnotationTarget.FUNCTION)
annotation class MyExperimentalAPI

open class Base {
    @MyExperimentalAPI
    open fun foo() {}
}

class Derived : Base() {
    override fun foo<caret>() {}
}
