// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtObjectDeclaration
// OPTIONS: usages
package server

public open class Server() {
    private object <caret>Foo {

    }

    open fun processRequest() = Foo
}

public class ServerEx(): Server() {
    override fun processRequest() = Server.Foo
}

// DISABLE-ERRORS