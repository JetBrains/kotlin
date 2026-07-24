// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: generated

// FILE: extension.kt
// RESOLVE_EXTENSION_FILE
package generated

// RESOLVE_EXTENSION_CALLABLE: generatedTopLevelExtensionFunction1
fun String.generatedTopLevelExtensionFunction1(boolean: Boolean): Int

// FILE: main.kt
import generated.*

fun main() {
    "string".generatedTopLeve<caret>lExtensionFunction1(true)
}