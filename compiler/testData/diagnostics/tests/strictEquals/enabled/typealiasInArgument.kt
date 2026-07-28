// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

class NonGeneric {
    typealias Self = NonGeneric
    override fun equals(@EqualityBound(Self::class) other: Any?): Boolean = true
}

class StarGeneric<K> {
    typealias Self = StarGeneric<*>
    override fun equals(@EqualityBound(Self::class) other: Any?): Boolean = true
}

class StarGeneric2<K> {
    typealias Self<K> = StarGeneric2<*>
    override fun equals(@EqualityBound(Self::class) other: Any?): Boolean = true
}

typealias Self<M> = NiceGeneric<M>
class NiceGeneric<L> {
    override fun equals(@EqualityBound(Self::class) other: Any?): Boolean = true
}

class BadGeneric<N> {
    typealias Self = BadGeneric<String>
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_ARGUMENT_EXPANDS_TO_NON_STAR_PROJECTED!>Self<!>::class) other: Any?): Boolean = true
}

class BadGeneric2<N> {
    typealias Self = BadGeneric2<String>
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_ARGUMENT_EXPANDS_TO_NON_STAR_PROJECTED!>Self<!>::class) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, operator, starProjection,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
