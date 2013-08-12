// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages
package testing;

public class Server() {
    public fun <caret>processRequest() = "foo"
}
