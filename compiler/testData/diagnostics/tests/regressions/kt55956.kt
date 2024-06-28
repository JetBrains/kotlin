// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:MyExperimentalAPI<!>

@RequiresOptIn
@Target(AnnotationTarget.CLASS)
annotation class MyExperimentalAPI

@MyExperimentalAPI
class Some {
    fun foo() {}
}

class Bar {
    @OptIn(MyExperimentalAPI::class)
    fun bar() {
        Some().foo()
    }
}

fun main(args: Array<String>) {}
