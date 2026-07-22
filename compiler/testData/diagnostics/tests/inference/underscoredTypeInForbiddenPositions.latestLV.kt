// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -UNCHECKED_CAST
// WITH_STDLIB

fun <K, T> foo(x: (K) -> T): Pair<K, T> = (1 as K) to (1f as T)

class Foo<K>

class Bar0<K : <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!><<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>>
class Bar1<K : Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>>
class Bar2<K : <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>
class Bar3<K> where K : <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>
class Bar4<<!UNDERSCORE_IS_RESERVED!>_<!>>

typealias A1<<!UNDERSCORE_IS_RESERVED!>_<!>> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS, UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>
typealias A11<<!UNDERSCORE_IS_RESERVED!>_<!>> = Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>
typealias A12<<!UNDERSCORE_IS_RESERVED!>_<!>> = Foo<Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>>
typealias A2<T> = Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>
typealias A3<T> = (<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>) -> T
typealias A4<T> = (T) -> () -> <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>
typealias A5<T> = (T) -> (((<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>))) -> T

fun foo1(x: <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>) {}
fun foo2(x: Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>) {}
fun foo21(x: Foo<Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>>) {}
fun foo3(): <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun foo4(): Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun foo5(): Foo<Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K, <!UNDERSCORE_IS_RESERVED!>_<!>> foo6(): Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K : <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>, <!UNDERSCORE_IS_RESERVED!>_<!>> foo7(): <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K : Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>, <!UNDERSCORE_IS_RESERVED!>_<!>> foo8(): Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <K : Foo<Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>>, <!UNDERSCORE_IS_RESERVED!>_<!>> foo9(): Foo<Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>.foo10() {}
fun Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>.foo11() {}
fun Foo<Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>>.foo12() {}

class AA1 : <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>
class AA2 : <!FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF, PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>><!>

fun <`_`> bar(): Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>> = TODO()
fun <`_`> bar1(): Foo<Foo<<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>>> = TODO()

fun test() {
    val x1 = foo<Int, (<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>) -> Unit> { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>{ it }<!> }
    val x1cp = foo<Int, context(<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>) () -> Unit> { <!CANNOT_INFER_IT_PARAMETER_TYPE!>{ "" }<!> }
    val x2 = foo<Int, (Int) -> <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>> { <!UNRESOLVED_REFERENCE!>{ it }<!> }
    val x3 = foo<Int, ((<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>)) -> <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>> { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>{ it }<!> }
    val x4 = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, FUNCTION_CALL_EXPECTED, NO_VALUE_FOR_PARAMETER!>foo<!><!UNRESOLVED_REFERENCE!><<!>Int<!SYNTAX!>, _ -> Float><!> <!CANNOT_INFER_IT_PARAMETER_TYPE!>{ { <!UNRESOLVED_REFERENCE!>it<!> } }<!>
    val x5 = foo<Int, Foo<(<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>) -> Float>> { <!RETURN_TYPE_MISMATCH!>{ it }<!> }
    val x6 = foo<Int, Foo<(<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>) -> <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>> { <!RETURN_TYPE_MISMATCH!>{ it }<!> }
    val x7 = foo<Int, Foo<(Int) -> <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>> { <!RETURN_TYPE_MISMATCH!>{ it }<!> }

    val z32: Pair<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>, Float> = 1 to 1f
    val z34: Pair<((<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>)), Float> = 1 to 1f

    val x8: (Float) -> Int = { x: <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!> -> 10 }
    val x9: (Foo<Float>) -> Int = { x: Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>> -> 10 }

    val x10 = object : <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!> {}
    val x11 = object : <!FINAL_SUPERTYPE!>Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>><!>() {}

    if (x11 is <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>) { }
    if (<!USELESS_IS_CHECK!>x11 is Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>><!>) { }

    x10 as <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>
    x10 <!CAST_NEVER_SUCCEEDS!>as<!> Foo<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>

    val x12: Foo<@<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!> Int>? = null
    val x13: Foo<@<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>() Int>? = null
    val x14: Foo<@Anno(<!UNRESOLVED_REFERENCE!>_<!>) Int>? = null

    val x15: <!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!><<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>? = null
}

@Target(AnnotationTarget.TYPE)
annotation class Anno(val x: Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, asExpression, classDeclaration,
comparisonExpression, funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression, integerLiteral,
isExpression, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, stringLiteral,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter, typeWithContext */
