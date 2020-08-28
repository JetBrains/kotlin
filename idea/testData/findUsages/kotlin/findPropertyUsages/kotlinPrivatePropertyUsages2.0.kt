// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages
package server

public open class Server(private val <caret>foo: String = "foo") {
    open fun processRequest() = foo
}

public class ServerEx(): Server(foo = "!") {
    override fun processRequest() = "foo" + foo // this reference is found as a side effect of big use scope of constructor parameter:
                                                // if it was simple property, it wouldn't be found
}
// DISABLE-ERRORS

