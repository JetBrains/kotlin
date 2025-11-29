// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555

class C {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    companion object {
        fun bar() { }
    }

    fun foo() { }
}

typealias T = C

fun test() {
    val ref = C::foo
    val wrongRef = <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>C::bar<!>
    val typealiasRef = T::foo
    val wrongTypealiasRef = T::bar
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, localProperty,
objectDeclaration, propertyDeclaration, stringLiteral, typeAliasDeclaration */
