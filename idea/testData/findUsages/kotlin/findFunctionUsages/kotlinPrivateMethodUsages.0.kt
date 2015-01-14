// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
package server

public open class Server() {
    private fun <caret>doProcessRequest() = "foo"

    open fun processRequest() = doProcessRequest()
}

public class ServerEx(): Server() {
    override fun processRequest() = "foo" + doProcessRequest()
}

