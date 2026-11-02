// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: String = "")

@Target(AnnotationTarget.TYPE)
annotation class Other

val annotated: @Anno context(Int)<!SYNTAX!><!>
val annotatedWithType: @Anno context(Int) String<!SYNTAX!><!>
val annotatedUnresolved: @Anno context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>
val annotatedUnresolvedType: @Anno context(Int) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>

fun after() {}

interface Forms {
    val simple: @Anno context(Int)<!SYNTAX!><!>
    val withArguments: @Anno("1" + "2") context(Int)<!SYNTAX!><!>
    val several: @Anno @Other context(Int)<!SYNTAX!><!>
    val bracketed: @[Anno Other] context(Int)<!SYNTAX!><!>
    val useSiteTarget: <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Anno<!> context(Int)<!SYNTAX!><!>
    val suspendAfter: @Anno suspend context(Int)<!SYNTAX!><!>
    val suspendBefore: suspend @Anno context(Int)<!SYNTAX!><!>
}

interface InsideList {
    val inside: context(@Anno Int)<!SYNTAX!><!>
    val insideUnresolved: context(@Anno <!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>
    val bothSides: @Anno context(@Other Int, @Anno("x") String)<!SYNTAX!><!>
    val named: @Anno context(a: @Other Int)<!SYNTAX!><!>
}

interface TypeAfterList {
    val type: @Anno context(Int) String<!SYNTAX!><!>
    val unresolvedType: @Anno context(Int) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>
    val nullableType: @Anno context(Int) String?<!SYNTAX!><!>
}

interface NullableAndParenthesized {
    val nullable: (@Anno context(Int)<!SYNTAX!><!>)?
    val nullableWithoutAnnotation: (context(Int)<!SYNTAX!><!>)?
    val nullableWithType: (@Anno context(Int) String<!SYNTAX!><!>)?
    val nullableWithReceiver: (@Anno context(Int) (String).<!SYNTAX!><!>)?
    val doubleNullable: ((@Anno context(Int)<!SYNTAX!><!>)?)?
    val nullableInsideDangling: @Anno context(Int) (@Other context(Long)<!SYNTAX!><!>)?<!SYNTAX!><!>
    val nullableWithoutType: (<!SYNTAX!><!>)?
    val nullableUnresolved: (@Anno context(<!UNRESOLVED_REFERENCE!>Unresolved<!>) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>)?
    val nullableAnnotatedEntry: (context(@Other Int)<!SYNTAX!><!>)?
    val nullableNullableType: (context(Int) String?<!SYNTAX!><!>)?
    val nullableSuspend: (suspend context(Int)<!SYNTAX!><!>)?
    val suspendInsideDangling: context(Int) (suspend context(Long)<!SYNTAX!><!>)?<!SYNTAX!><!>
    val suspendOutsideDangling: suspend context(Int) (context(Long)<!SYNTAX!><!>)?<!SYNTAX!><!>
    val nullableAnnotatedEntryUnresolved: (context(@Other <!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>)?
    val parenthesized: (@Anno context(Int)<!SYNTAX!><!>)
}

interface Nested {
    val typeArgument: List<@Anno context(Int)<!SYNTAX!><!>>
    val functionTypeParameter: (@Anno context(Int)<!SYNTAX!><!>) -> Unit
    val functionTypeReturn: () -> @Anno context(Int)<!SYNTAX!><!>
}

fun (@Anno context(Int)<!SYNTAX!><!>).receiver() {}

fun parameter(p: @Anno context(Int)<!SYNTAX!><!>) {}

fun returnType(): @Anno context(Int)<!SYNTAX!><!> = TODO()

fun <T : @Anno context(Int)<!SYNTAX!><!>> bound() {}

fun <T> whereConstraint() where T : @Anno context(Int)<!SYNTAX!><!> {}

typealias Alias = @Anno context(Int)<!SYNTAX!><!>

interface Supertype : @Anno context(Int)<!SYNTAX!><!> {}

data class Data(val p: @Anno context(Int)<!SYNTAX!><!>)

class Setter {
    var p: Int = 0
        set(value: @Anno context(Int)<!SYNTAX!><!>) {}
}

fun body(a: Any) {
    val local: @Anno context(Int)<!SYNTAX!><!> = TODO()
    val lambda = { p: @Anno context(Int)<!SYNTAX!><!> -> }
    val anonymousFunction = fun(p: @Anno context(Int)<!SYNTAX!><!>) {}
    val asTarget = (a as @Anno context(Int)<!SYNTAX!><!>)
    val isTarget = (a is @Anno context(Int)<!SYNTAX!><!>)
    try {} catch (e: @Anno context(Int)<!SYNTAX!><!>) {}
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetReceiver, anonymousFunction, asExpression,
classDeclaration, data, funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral,
interfaceDeclaration, isExpression, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration,
setter, stringLiteral, tryExpression, typeAliasDeclaration, typeConstraint, typeParameter */
