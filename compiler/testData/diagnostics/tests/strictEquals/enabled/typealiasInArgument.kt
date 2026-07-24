// RUN_PIPELINE_TILL: BACKEND

class NonGeneric {
    typealias Self = NonGeneric
    override fun equals(@EqualityBound(Self::class) other: Any?): Boolean = true
}

class StarGeneric<K> {
    typealias Self = StarGeneric<*>
    override fun equals(@EqualityBound(Self::class) other: Any?): Boolean = true
}

typealias Self<M> = NiceGeneric<M>
class NiceGeneric<L> {
    override fun equals(@EqualityBound(Self::class) other: Any?): Boolean = true
}

class BadGeneric<N> {
    typealias Self = BadGeneric<String>
    override fun equals(@EqualityBound(Self::class) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, operator, starProjection,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
