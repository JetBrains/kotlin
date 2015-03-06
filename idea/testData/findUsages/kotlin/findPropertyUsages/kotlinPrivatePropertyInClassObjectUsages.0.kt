// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetProperty
// OPTIONS: usages
class Outer {
    val x = Outer.t

    default object {
        private val <caret>t = 1
    }
}

