// RUN_PIPELINE_TILL: FRONTEND
error class E

typealias ListRes<T> = List<T> | E

class In<in T> {
    fun okList(x: ListRes<T>) {}
    fun badList(): ListRes<<!TYPE_VARIANCE_CONFLICT_ERROR!>T<!>> = E()
}

class Out<out T> {
    fun okList(): ListRes<T> = E()
    fun badList(x: ListRes<<!TYPE_VARIANCE_CONFLICT_ERROR!>T<!>>) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, in, nullableType, out, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
