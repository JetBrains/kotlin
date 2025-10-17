// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals
// DIAGNOSTICS: -REDUNDANT_CALL_OF_CONVERSION_METHOD

class MyList<T> {
    companion object {
        operator fun <T1> of(vararg vals: T1): MyList<T1> = MyList<T1>()
    }
}

fun returnString() = ""
fun returnNullableString(): String? = null
fun String.twice() = this + this
fun <U> runLike(block: () -> U) = block()
fun runLikeString(block: () -> String) = block()
fun <K> materialize(): K = null!!
fun <H> id(h: H) = h

fun takeMyList(lst: MyList<String>) { }

fun test() {
    val s = ""
    takeMyList(["1", "2", "3"])
    takeMyList(["" + "42"])
    takeMyList(["" + 42])
    takeMyList([42.toString()])
    takeMyList([MyList<Int>().toString()])
    takeMyList([s])
    takeMyList([null!!])
    takeMyList([returnNullableString()!!])
    takeMyList([
        when (val str = returnNullableString()) {
            null -> returnString()
            else -> str.twice()
        }
    ])
    takeMyList([returnNullableString()?.twice().toString()])
    takeMyList([returnNullableString()?.twice() ?: ""])
    takeMyList([runLikeString { "42" }])
    takeMyList([id("42")])
    takeMyList([materialize()])
    takeMyList([runLike { returnString().twice() }])
    takeMyList([returnNullableString() as String])
    val smart = returnNullableString()
    if (smart != null) takeMyList([smart])

    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[returnNullableString()]<!>)
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[returnNullableString()?.toString()]<!>)
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[returnNullableString()?.twice()]<!>)
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[
        when (val str = returnNullableString()) {
            null -> returnString()
            else -> returnNullableString()
        }
    ]<!>)
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>["" as Any?]<!>)
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[runLike { 42 }]<!>)
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[runLike { returnNullableString()?.twice() }]<!>)
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[id(42)]<!>)

    takeMyList([<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_IT_PARAMETER_TYPE!>{ str: String -> str }<!>])
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[""::twice]<!>)
    takeMyList(<!ARGUMENT_TYPE_MISMATCH!>[String::twice]<!>)
}

/* GENERATED_FIR_TAGS: additiveExpression, asExpression, callableReference, checkNotNullCall, classDeclaration,
companionObject, elvisExpression, equalityExpression, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, integerLiteral, lambdaLiteral, localProperty, nullableType, objectDeclaration, operator,
propertyDeclaration, safeCall, smartcast, stringLiteral, thisExpression, typeParameter, vararg, whenExpression,
whenWithSubject */
