// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
package anonymousUnused

fun main(args: Array<String>) {
    val a = object {
        val b = object {
            val c = object {
                val <caret>d = 5
            }
        }
    }

    a.b.c.d
}