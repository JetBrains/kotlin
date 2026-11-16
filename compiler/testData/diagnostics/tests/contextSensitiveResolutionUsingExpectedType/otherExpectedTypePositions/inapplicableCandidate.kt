// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89779
// LANGUAGE: +ContextParameters

// FILE: declarations.kt
package ppp

class C {
    companion object {
        val onlyInapplicable: C = C()
        val inapplicableAndInvisible: C = C()
        val invisibleInapplicable: C = C()
    }
}

class Context<T>

context(_: Context<Int>)
val onlyInapplicable: C
    get() = C()

context(_: Context<Int>)
val inapplicableAndInvisible: C
    get() = C()

// FILE: privates.kt
package ppp

private val inapplicableAndInvisible: C = C()

context(_: Context<Int>)
private val invisibleInapplicable: C
    get() = C()

// FILE: main.kt
package ppp

fun test() {
    val a: C = onlyInapplicable
    val b: C = inapplicableAndInvisible
    val c: C = invisibleInapplicable
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, localProperty, nullableType,
objectDeclaration, propertyDeclaration, propertyDeclarationWithContext, typeParameter */
