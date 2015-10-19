// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
package anonymousUnused

fun main(args: Array<String>) {
    val localObject = object : Any() {
        fun <caret>f() {
        }
    }

    localObject.f()
}