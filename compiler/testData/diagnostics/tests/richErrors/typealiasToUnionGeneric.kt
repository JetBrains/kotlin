// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
// WITH_STDLIB
error class E

typealias ListRes<T> = List<T> | E

fun resString(): ListRes<String> = listOf("")
fun resNullableString(): ListRes<String?> = listOf(null)
fun listRes(l: List<Int>): ListRes<Int> = l
fun listResOfE(l: List<E>): ListRes<E> = l
fun nestedListRes(): ListRes<ListRes<Int>> = listOf(listOf(1))
fun <T> id(x: ListRes<T>): ListRes<T> = x
fun star(x: ListRes<*>) { }
fun inProjection(x: ListRes<in Int>) {}
fun mapOfListRes(m: Map<String, ListRes<Int>>) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, stringLiteral, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
