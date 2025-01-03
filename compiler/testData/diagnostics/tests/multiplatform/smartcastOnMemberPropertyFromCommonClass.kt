// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Some<!> {
    val e: SomeEnum? = null
}

enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SomeEnum<!> {
    A, B
}

// MODULE: main()()(common)
fun Some.test() {
    if (e == null) return
    val x = when (<!DEBUG_INFO_SMARTCAST!>e<!>) {
        SomeEnum.A -> "a"
        SomeEnum.B -> "B"
    }
}
