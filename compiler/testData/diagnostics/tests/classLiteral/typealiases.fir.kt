// RUN_PIPELINE_TILL: FRONTEND
typealias TString = String
fun f1() = TString::class

typealias TNullableString = String?
fun f2() = TNullableString::class

typealias TNullableTString = TString?
typealias TTNullableTString = TNullableTString
fun f3() = TTNullableTString::class

inline fun <reified T> f4(b: Boolean): Any {
    typealias X = T
    typealias Y = T?
    return if (b) X::class else Y::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, ifExpression, inline, nullableType, reified,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
