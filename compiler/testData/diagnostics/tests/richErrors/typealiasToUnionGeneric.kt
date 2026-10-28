// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// WITH_STDLIB
error class E

typealias ListRes<T> = List<T> | E

fun resString(): ListRes<String> = listOf("")
fun resNullableString(): ListRes<String?> = listOf(null)
fun listRes(l: List<Int>): ListRes<Int> = l
fun nestedListRes(): ListRes<ListRes<Int>> = <!RETURN_TYPE_MISMATCH!>listOf(listOf(""))<!>
fun <T> id(x: ListRes<T>): ListRes<T> = x
fun star(x: ListRes<*>) { }
fun inProjection(x: ListRes<in Int>) {}
fun mapOfListRes(m: Map<String, <!UNRESOLVED_REFERENCE!>Res<!><Int>>) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, stringLiteral, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
