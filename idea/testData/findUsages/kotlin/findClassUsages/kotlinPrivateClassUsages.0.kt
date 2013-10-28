// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: usages, constructorUsages
package server;

public open class Server() {
    private class <caret>Foo {

    }

    open fun processRequest() = Foo()
}

public class ServerEx(): Server() {
    override fun processRequest() = Server.Foo()
}

