// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>()

// MODULE: intermediate()()(common)
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!> : A() {
    fun foo(): String = "O"
}

<!CONFLICTING_OVERLOADS!>fun getB(): B<!> = B()

// MODULE: main()()(intermediate)
actual open class A actual constructor() {
    fun bar(): String = "K"
}

fun box(): String {
    val b = getB()
    return b.foo() + b.bar()
}
