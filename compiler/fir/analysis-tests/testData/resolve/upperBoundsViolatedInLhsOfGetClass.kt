// ISSUE: KT-84585
// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT

class Inv<S>
class C<X, Y : X>
typealias TA<T> = C<Inv<String>, Inv<T>>
typealias TA2<T> = C<Int, List<T>>
typealias TA3<T> = C<List<T>, List<String>>
typealias TA4<T> = C<List<T>, List<T>>
typealias TA5<T> = C<MutableList<T>, List<T>>
typealias TA6<T> = C<List<T>, MutableList<T>>

fun test() {
    <!UPPER_BOUND_VIOLATED_IN_LHS_OF_CLASS_LITERAL_WARNING!>TA<!>::class
    <!UPPER_BOUND_VIOLATED_IN_LHS_OF_CLASS_LITERAL_WARNING!>TA2<!>::class
    TA3::class
    TA4::class
    <!UPPER_BOUND_VIOLATED_IN_LHS_OF_CLASS_LITERAL_WARNING!>TA5<!>::class
    TA6::class
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
