// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: generated

// FILE: resolve1.kt
// RESOLVE_EXTENSION_FILE
package generated

// RESOLVE_EXTENSION_CLASSIFIER: GeneratedClass1
class GeneratedClass1

// FILE: resolve2.kt
// RESOLVE_EXTENSION_FILE
package generated

// RESOLVE_EXTENSION_CLASSIFIER: GeneratedClass2
class GeneratedClass2() {
    fun generatedClassMember2(): GeneratedClass1 = TODO()
}

// FILE: main.kt
import generated.*

fun main() {
    val a = GeneratedClass2()
    a.gener<caret>atedClassMember2()
}