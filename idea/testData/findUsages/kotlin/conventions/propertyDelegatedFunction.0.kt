// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages

class Delegate() {
    fun getValue(thisRef: Any?, propertyMetadata: PropertyMetadata): String = ":)"
    fun setValue(thisRef: Any?, propertyMetadata: PropertyMetadata, value: String) {
    }

    fun <caret>propertyDelegated(propertyMetadata: PropertyMetadata) {
    }
}

var p: String by Delegate()
