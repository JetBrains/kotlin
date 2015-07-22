fun foo() {
    a<caret>
}

val a by Delegate()

class Delegate {
    fun get(t: Any?, p: PropertyMetadata) = 1
}

// EXISTS: a.get(Any?\, PropertyMetadata)