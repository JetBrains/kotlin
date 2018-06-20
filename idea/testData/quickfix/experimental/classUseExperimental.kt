// "Add '@UseExperimental(MyExperimentalAPI::class)' annotation to containing class 'Bar'" "true"
// COMPILER_ARGUMENTS: -Xuse-experimental=kotlin.Experimental
// WITH_RUNTIME

package a.b

@Experimental
@Target(AnnotationTarget.FUNCTION)
annotation class MyExperimentalAPI

@MyExperimentalAPI
fun foo() {}

class Bar {
    fun bar() {
        foo<caret>()
    }
}