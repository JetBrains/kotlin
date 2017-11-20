// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtSecondaryConstructor
// OPTIONS: usages
open class B {
    constructor(): this("") {

    }

    internal <caret>constructor(s: String) {

    }
}

open class A : B {
    constructor(a: Int) : super("") {

    }
}

class C: B("") {

}

fun test() {
    B("")
}