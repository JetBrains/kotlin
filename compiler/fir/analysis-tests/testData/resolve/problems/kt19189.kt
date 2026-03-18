// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-19189
// WITH_STDLIB

// KT-19189: AssertionError: Recursion detected on input when class has method with same name as parent class method

open class ComponentBuilder<B : ComponentBuilder<B>>(val name: String = "") {
    fun keybind(key: String, block: KeybindComponentBuilder.() -> Unit = {}) = apply {
        KeybindComponentBuilder().keybind(key).apply(block)
    }
}

class KeybindComponentBuilder(keybind: String? = null) : ComponentBuilder<KeybindComponentBuilder>() {
    init {
        if (keybind != null) keybind(keybind)
    }

    fun keybind(keybind: String) = apply { }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, functionalType, ifExpression, init,
lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, smartcast, stringLiteral, typeConstraint,
typeParameter, typeWithExtension */