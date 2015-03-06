// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages

class Delegate() {
    fun get(thisRef: Any?, propertyMetadata: PropertyMetadata): String = ":)"
    fun <caret>set(thisRef: Any?, propertyMetadata: PropertyMetadata, value: String) {

    }
}

var p: String by Delegate()
