fun foo() {
    a<caret>
}

val a by Delegate()

class Delegate {
    fun getValue(t: Any?, p: PropertyMetadata) = 1
}

// EXISTS: a.getValue(Any?\, PropertyMetadata)