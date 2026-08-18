// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

open class P1<T>

class C1<T> : P1<String>() {
    override fun equals(@EqualityBound(P1::class) other: Any?): Boolean = true
}

open class P2<in T>

class C2 : P2<CharSequence>() {
    override fun equals(@EqualityBound(P2::class) other: Any?): Boolean = true
}

class C3<in X> {
    override fun equals(@EqualityBound(C3::class) other: Any?): Boolean = true
}

class Unrelated

class C4 {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>Unrelated<!>::class) other: Any?): Boolean = true
}

open class C5<X> {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>S5<!>::class) other: Any?): Boolean = true
}

class S5 : C5<CharSequence>()

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, operator, override,
typeParameter */
