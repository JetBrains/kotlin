// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
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

// DISABLE-ERRORS