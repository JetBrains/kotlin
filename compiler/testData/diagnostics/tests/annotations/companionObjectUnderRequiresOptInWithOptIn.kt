// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-82524

@RequiresOptIn
annotation class Marker

class A {
    @Marker
    companion object {
        val a = A
    }
}

@Marker
fun withMarker() {
    val a = A
}

@OptIn(Marker::class)
fun withOptIn() {
    val a = A
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, functionDeclaration, localProperty,
objectDeclaration, propertyDeclaration */
