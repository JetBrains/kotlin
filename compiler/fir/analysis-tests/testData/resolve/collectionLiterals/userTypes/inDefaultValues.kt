// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// DIAGNOSTICS: -UNCHECKED_CAST
// RENDER_DIAGNOSTICS_FULL_TEXT

class MyList<T> {
    companion object {
        operator fun <T> of(vararg vals: T) = MyList<T>()
    }
}

typealias Nested = MyList<MyList<String>>

class InConstructor(
    val a: MyList<String> = [],
    val b: MyList<String> = ["1", "2", "3"],
    c: MyList<String> <!INITIALIZER_TYPE_MISMATCH!>=<!> [1, 2, 3],
)

class InGenericConstructor<T>(
    val a: MyList<T> = [],
    b: MyList<T> <!INITIALIZER_TYPE_MISMATCH!>=<!> [1, 2, 3],
    val c: MyList<T> = [null as T],
)

fun inFunction(
    a: MyList<String> = [],
    b: MyList<Int> = run { [1, 2, 3] } ,
    c: MyList<String> <!INITIALIZER_TYPE_MISMATCH!>=<!> [null, "0"],
    d: Nested = [[]],
    e: Nested = [<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>],
    f: Nested = [["1", "2", "3"]],
    g: Nested = [[<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>]],
    h: Nested = [],
) = Unit

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, collectionLiteral, companionObject, functionDeclaration,
integerLiteral, lambdaLiteral, nullableType, objectDeclaration, operator, primaryConstructor, propertyDeclaration,
stringLiteral, typeAliasDeclaration, typeParameter, vararg */
