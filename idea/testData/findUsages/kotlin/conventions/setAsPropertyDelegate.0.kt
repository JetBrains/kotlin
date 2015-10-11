// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages

class Delegate() {
    fun getValue(thisRef: Any?, propertyMetadata: PropertyMetadata): String = ":)"
    fun <caret>setValue(thisRef: Any?, propertyMetadata: PropertyMetadata, value: String) {

    }
}

var p: String by Delegate()
