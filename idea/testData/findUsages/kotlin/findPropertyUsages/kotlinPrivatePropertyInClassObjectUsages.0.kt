// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetProperty
// OPTIONS: usages
class Outer {
    val x = Outer.t

    class object {
        private val <caret>t = 1
    }
}

