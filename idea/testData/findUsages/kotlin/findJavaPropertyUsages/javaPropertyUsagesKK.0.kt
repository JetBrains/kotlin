// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// CHECK_SUPER_METHODS_YES_NO_DIALOG: no
// OPTIONS: usages
open class A {
    open var p: Int = 1
}

class AA : A() {
    override var <caret>p: Int = 1
}

class B : J() {
    override var p: Int = 1
}

fun test() {
    val t = A().p
    A().p = 1

    val t = AA().p
    AA().p = 1

    val t = J().p
    J().p = 1

    val t = B().p
    B().p = 1
}

// DISABLE-ERRORS