// MODULE: extendedModule
// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: generated

// FILE: extension.kt
// RESOLVE_EXTENSION_FILE
package generated

// RESOLVE_EXTENSION_CLASSIFIER: GeneratedClass1
class GeneratedClass1

// RESOLVE_EXTENSION_CALLABLE: generatedTopLevelFunction1
fun generatedTopLevelFunction1(): GeneratedClass2

// FILE: extension2.kt
// RESOLVE_EXTENSION_FILE
package generated

// RESOLVE_EXTENSION_CLASSIFIER: GeneratedClass2
class GeneratedClass2 {
    fun generatedClassMember2(): GeneratedClass1
}

// MODULE: dependency2

// MODULE: main(extendedModule, dependency2)()()
import generated.*

fun main() {
    generatedTopLevelFunc<caret>tion1()
}
