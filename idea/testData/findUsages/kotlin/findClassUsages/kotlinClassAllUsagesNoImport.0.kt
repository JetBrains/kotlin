// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages, skipImports
package server

open class <caret>Server {
    open fun work() {
        println("Server")
    }
}
