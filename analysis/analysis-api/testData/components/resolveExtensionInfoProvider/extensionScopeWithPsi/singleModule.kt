// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: generated

// FILE: extension1.kt
// RESOLVE_EXTENSION_FILE
package generated

// RESOLVE_EXTENSION_CLASSIFIER: GenClass1
class GenClass1 {
    fun genMemberFun1(foo: Any): String = TODO()
    fun GenClass2.genMemberExtension1(): Unit = TODO()

    val genMemberVal1: String = "foo"

    var genMemberVar1: String = "bar"
        private set
}

// RESOLVE_EXTENSION_CALLABLE: genTopLevelFun1
fun genTopLevelFun1(foo: GenClass2): String = TODO()

// RESOLVE_EXTENSION_CALLABLE: genTopLevelExtension1
fun String.genTopLevelExtension1(): Int = TODO()

// RESOLVE_EXTENSION_CALLABLE: genTopLevelVal1
val genTopLevelVal1: String = "baz"

// RESOLVE_EXTENSION_CALLABLE: genTopLevelVar1
var genTopLevelVar1: String = "quux"
    internal set

// RESOLVE_EXTENSION_CALLABLE: genExtensionVal1
val GenClass2.genExtensionVal1: Int
    get() = TODO

// FILE: extension2.kt
// RESOLVE_EXTENSION_FILE
package generated

// RESOLVE_EXTENSION_CLASSIFIER: GenClass2
open class GenClass2

// FILE: main.kt
fun <caret_onAirContext>main() {}