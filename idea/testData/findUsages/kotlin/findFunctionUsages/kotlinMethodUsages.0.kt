// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
package server

public open class Server() {
    open fun <caret>processRequest() = "foo"
}

public class ServerEx(): Server() {
    override fun processRequest() = "foofoo"
}

