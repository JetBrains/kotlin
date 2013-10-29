// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetProperty
// OPTIONS: usages
package server;

public open class Server() {
    private val <caret>foo = "foo"

    open fun processRequest() = foo
}

public class ServerEx(): Server() {
    override fun processRequest() = "foo" + foo
}

