// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetParameter
// OPTIONS: usages
package server

public open class Server(private val <caret>foo: String = "foo") {
    open fun processRequest() = foo
}

public class ServerEx(): Server() {
    override fun processRequest() = "foo" + foo
}

