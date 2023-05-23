// MODULE: extendedModule
// FILE: generated.hidden.kt
package generated

fun String.generatedOverloadedExtensionFunction(): Int = TODO()

// MODULE: dependency2

// MODULE: main(extendedModule, dependency2)()()
// FILE: main.kt
import generated.*

fun main() {
    "string".generatedOverloadedExtension<caret>Function()
}