// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_VARIABLE
class IntController {
    operator fun handleResult(x: Int, c: Continuation<Nothing>) { }
}

class GenericController<T> {
    operator fun handleResult(x: T, c: Continuation<Nothing>) { }
}

class UnitController {
    operator fun handleResult(x: Unit, c: Continuation<Nothing>) { }
}

class EmptyController

fun builder(coroutine c: IntController.() -> Continuation<Unit>) = 1
fun <T> genericBuilder(coroutine c: GenericController<T>.() -> Continuation<Unit>): T = null!!
fun unitBuilder(coroutine c: UnitController.() -> Continuation<Unit>) = 1
fun emptyBuilder(coroutine c: EmptyController.() -> Continuation<Unit>) = 1

fun <T> manyArgumentsBuilder(
        coroutine c1: UnitController.() -> Continuation<Unit>,
        coroutine c2: GenericController<T>.() -> Continuation<Unit>,
        coroutine c3: IntController.() -> Continuation<Unit>
):T = null!!

fun severalParamsInLambda(coroutine c: UnitController.(String, Int) -> Continuation<Unit>) {}

fun foo() {
    builder({ 1 })
    builder { 1 }

    val x = { 1 }
    builder(<!TYPE_MISMATCH!>x<!>)
    builder(<!UNCHECKED_CAST!>{1} as (IntController.() -> Continuation<Unit>)<!>)

    var i: Int = 1
    i = genericBuilder({ 1 })
    i = genericBuilder { 1 }
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>genericBuilder<!> { 1 }
    genericBuilder<Int> { 1 }
    genericBuilder<Int> { <!TYPE_MISMATCH!>""<!> }

    val y = { 1 }
    <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>genericBuilder<!>(<!TYPE_MISMATCH!>y<!>)

    unitBuilder {}
    unitBuilder { <!UNUSED_EXPRESSION!>1<!> }
    unitBuilder({})
    unitBuilder({ <!UNUSED_EXPRESSION!>1<!> })

    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>manyArgumentsBuilder<!>({}, { "" }) { 1 }

    val s: String = manyArgumentsBuilder({}, { "" }) { 1 }

    manyArgumentsBuilder<String>({}, { "" }, { 1 })
    manyArgumentsBuilder<String>({}, { <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> }, { 2 })

    severalParamsInLambda { x, y ->
        x checkType { _<String>() }
        y checkType { _<Int>() }
    }
}
