// RUN_PIPELINE_TILL: FRONTEND
error class E

class NotNullBox<T : Any>

typealias Res<T> = NotNullBox<T> | E
typealias Nested<T> = List<NotNullBox<T>> | E

fun <T> f0() : NotNullBox<<!UPPER_BOUND_VIOLATED!>T<!>> | E = E()

fun f1(): Res<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String?<!>> = E()
fun f3(): Res<String> = E()
fun f4(x: List<Res<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String?<!>>>) {}
fun f4_(x: List<NotNullBox<<!UPPER_BOUND_VIOLATED!>String?<!>> | E>) {}
fun f5(): Nested<<!UPPER_BOUND_VIOLATED!>String?<!>> = E()
fun <T> f6(): Res<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>T<!>> = E()
fun <T : Any> f7(): Res<T> = E()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
