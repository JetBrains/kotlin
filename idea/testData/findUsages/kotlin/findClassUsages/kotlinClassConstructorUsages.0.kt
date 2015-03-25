// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: constructorUsages
package server

open class <caret>Server {
    constructor(name: String): this() {

    }

    companion object {
        val NAME = "Server"
    }

    open fun work() {
        println("Server")
    }
}
