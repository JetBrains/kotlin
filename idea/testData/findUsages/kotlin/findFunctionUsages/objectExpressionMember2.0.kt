// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
package anonymousUnused

fun main(args: Array<String>) {
    fun localObject() = object : Any() {
        fun <caret>f() {
        }
    }

    localObject().f()
}