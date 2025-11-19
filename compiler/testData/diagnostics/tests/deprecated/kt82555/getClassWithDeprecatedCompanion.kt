// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82555

class C {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    companion object
}

typealias T = C

fun test() {
    val ref = C::class
    val typealiasRef = T::class
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nestedClass, propertyDeclaration,
stringLiteral */