// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages

class Delegate() {
    fun <caret>get(thisRef: Any?, propertyMetadata: PropertyMetadata): String = ":)"
}

val p: String by Delegate()
