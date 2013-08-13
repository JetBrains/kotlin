// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: usages
package server

open class <caret>Server {
    class object {
        val NAME = "Server"
    }

    open fun work() {
        println("Server")
    }
}