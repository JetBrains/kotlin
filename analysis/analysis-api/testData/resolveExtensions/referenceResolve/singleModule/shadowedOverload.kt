// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: generated
// RESOLVE_EXTENSION_SHADOWED: \.hidden\.kt$

// FILE: extension.kt
// RESOLVE_EXTENSION_FILE
package generated

// RESOLVE_EXTENSION_CALLABLE: generatedOverloadedExtensionFunction
fun Any.generatedOverloadedExtensionFunction(): Int = TODO()

// FILE: generated.hidden.kt
package generated

fun String.generatedOverloadedExtensionFunction(): Int = TODO()

// FILE: main.kt
import generated.*

fun main() {
    "string".generatedOverloadedExtension<caret>Function()
}