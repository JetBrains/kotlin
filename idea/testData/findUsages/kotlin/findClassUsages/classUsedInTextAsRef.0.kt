// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: usages
package server

open class <caret>Server {
    default object {
        val NAME = "Server"
    }

    open fun work() {
        println("Server")
    }
}
