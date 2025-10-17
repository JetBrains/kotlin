// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-81141
// RENDER_DIAGNOSTICS_FULL_TEXT

@Repeatable
annotation class Foo(val arr: Array<String> = [])

typealias FooAlias = Foo

val foo = Foo(<!UNSUPPORTED!>["42"]<!>)

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
    Foo(<!UNSUPPORTED!>["42"]<!>)
    Foo(<!UNSUPPORTED!>[]<!>)
    Foo(run {
        val x = <!UNSUPPORTED!>["42"]<!>
        x
    })

    NestedFoo(*<!UNSUPPORTED!>[NestedFoo(arr = <!UNSUPPORTED!>[NestedFoo()]<!>)]<!>)

    IntFoo(<!UNSUPPORTED!>[]<!>)
    IntFoo(<!UNSUPPORTED!>[42]<!>)
    IntFoo(<!TYPE_MISMATCH!>run { <!UNSUPPORTED!>[42]<!> }<!>)
    IntFoo(<!TYPE_MISMATCH!>run {
        val x = <!UNSUPPORTED!>[42]<!>
        x
    }<!>)
    IntFoo(run {
        val x: IntArray = <!UNSUPPORTED!>[42]<!>
        x
    })

    FooAlias(<!UNSUPPORTED!>[]<!>)
    FooAlias(<!UNSUPPORTED!>["42"]<!>)

    val nonConst = "42"
    Foo(run { <!UNSUPPORTED!>[nonConst]<!> })

    <!UNSUPPORTED!>[nonConst]<!>.returnFoo(<!UNSUPPORTED!>[nonConst]<!>)
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, UNSUPPORTED!>[]<!>.returnFoo(<!UNSUPPORTED!>[]<!>)

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>returnFooGeneric<!>(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, UNSUPPORTED!>[]<!>)
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>returnFooGeneric<!>(<!UNSUPPORTED!>[42]<!>)
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>returnFooGeneric<!>(<!UNSUPPORTED!>["42"]<!>)

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>returnTypeParam<!>(foo, <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, UNSUPPORTED!>[]<!>)
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>returnTypeParam<!>(foo, <!UNSUPPORTED!>[1, 2, 3]<!>)
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>returnTypeParam<!>(foo, <!UNSUPPORTED!>["1", "2", "3"]<!>)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, lambdaLiteral, localProperty,
primaryConstructor, propertyDeclaration, stringLiteral */
