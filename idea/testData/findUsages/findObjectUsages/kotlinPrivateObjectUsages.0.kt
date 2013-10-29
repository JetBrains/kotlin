// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetObjectDeclarationName
// OPTIONS: usages
package server;

public open class Server() {
    private object <caret>Foo {

    }

    open fun processRequest() = Foo
}

public class ServerEx(): Server() {
    override fun processRequest() = Foo
}

