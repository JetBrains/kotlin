// RUN_PIPELINE_TILL: BACKEND
// SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
// influences only DEBUG_INFO_EXPRESSION_TYPE, so not important

typealias Action<K> = (@UnsafeVariance K) -> Unit
typealias Action2<K> = (@UnsafeVariance K) -> K

data class Tag<L>(val action: Action<L>)
data class Tag2<L>(val action: Action<<!REDUNDANT_PROJECTION!>in<!> L>)
data class Tag3<in L>(val action: Action<L>)
data class Tag4<in L>(val action: Action<<!REDUNDANT_PROJECTION!>in<!> L>)
data class Tag5<L>(val action: Action2<L>)
data class Tag6<out L>(val action: Action<<!REDUNDANT_PROJECTION!>in<!> L>)
data class Tag7<out L>(val action: Action<L>)
data class Tag8<out L>(val action: Action2<L>)

fun getTag(): Tag<*> = Tag<Int> {}
fun getTag2(): Tag2<*> = Tag2<Int> {}
fun getTag3(): Tag3<*> = Tag3<Int> {}
fun getTag4(): Tag4<*> = Tag4<Int> {}
fun getTag5(): Tag5<*> = Tag5<Int> { 1 }
fun getTag6(): Tag6<*> = Tag6<Int> { }
fun getTag7(): Tag7<*> = Tag7<Int> { }
fun getTag8(): Tag8<*> = Tag8<Int> { 1 }

fun main() {
    <!DEBUG_INFO_EXPRESSION_TYPE("(CapturedType(*)) -> kotlin.Unit")!>getTag().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(in CapturedType(*)) -> kotlin.Unit")!>getTag2().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(CapturedType(*)) -> kotlin.Unit")!>getTag3().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(in CapturedType(*)) -> kotlin.Unit")!>getTag4().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(CapturedType(*)) -> CapturedType(*)")!>getTag5().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(in kotlin.Any?) -> kotlin.Unit")!>getTag6().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Any?) -> kotlin.Unit")!>getTag7().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Any?) -> kotlin.Any?")!>getTag8().action<!>
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, data, functionDeclaration, functionalType, in, inProjection,
integerLiteral, lambdaLiteral, nullableType, out, primaryConstructor, propertyDeclaration, starProjection,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
