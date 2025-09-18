// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-81141
// RENDER_DIAGNOSTICS_FULL_TEXT

@Repeatable
annotation class Foo(val arr: Array<String> = [])

typealias FooAlias = Foo

val foo = Foo(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>)

fun Array<String>.returnFoo(param: Array<String>) = Foo(param)
fun <T> returnFooGeneric(param: T) = foo
fun <A, B> returnTypeParam(a: A, b: B) = a

annotation class NestedFoo(vararg val arr: NestedFoo = [NestedFoo(), NestedFoo(*[])])
annotation class IntFoo(val arr: IntArray)

@Foo(["42"])
@FooAlias([])
@NestedFoo(*[NestedFoo(arr = [NestedFoo()])])
@IntFoo([42])
fun test() {
    Foo(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>)
    Foo(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>)
    Foo(run {
        val x = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        x
    })

    NestedFoo(*<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[NestedFoo(arr = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[NestedFoo()]<!>)]<!>)

    IntFoo(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>)
    IntFoo(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[42]<!>)
    IntFoo(<!ARGUMENT_TYPE_MISMATCH!>run { <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[42]<!> }<!>)
    IntFoo(<!ARGUMENT_TYPE_MISMATCH!>run {
        val x = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[42]<!>
        x
    }<!>)
    IntFoo(run {
        val x: IntArray = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[42]<!>
        x
    })

    FooAlias(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>)
    FooAlias(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>)

    val nonConst = "42"
    Foo(run { <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[nonConst]<!> })

    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[nonConst]<!>.returnFoo(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[nonConst]<!>)
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>returnFoo<!>(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>)

    returnFooGeneric(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>)
    returnFooGeneric(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[42]<!>)
    returnFooGeneric(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>)

    returnTypeParam(foo, <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>)
    returnTypeParam(foo, <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>)
    returnTypeParam(foo, <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["1", "2", "3"]<!>)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, lambdaLiteral, localProperty,
primaryConstructor, propertyDeclaration, stringLiteral */
