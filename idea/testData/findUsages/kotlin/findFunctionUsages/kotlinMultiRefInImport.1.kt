// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
package client

import server.foo

fun test() {
    foo()
    foo(1)
    val t = foo
}
