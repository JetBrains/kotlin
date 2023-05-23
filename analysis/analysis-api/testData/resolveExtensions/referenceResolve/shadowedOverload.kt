// FILE: generated.hidden.kt
package generated

fun String.generatedOverloadedExtensionFunction(): Int = TODO()

// FILE: main.kt
import generated.*

fun main() {
    "string".generatedOverloadedExtension<caret>Function()
}