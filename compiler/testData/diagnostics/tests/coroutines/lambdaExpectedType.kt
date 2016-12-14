// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_VARIABLE

fun builder(c: @Suspend() (() -> Int)) = 1
fun <T> genericBuilder(c: @Suspend() (() -> T)): T = null!!
fun unitBuilder(c: @Suspend() (() -> Unit)) = 1
fun emptyBuilder(c: @Suspend() (() -> Unit)) = 1

fun <T> manyArgumentsBuilder(
        c1: @Suspend() (() -> Unit),
        c2: @Suspend() (() -> T),
        c3: @Suspend() (() -> Int)
):T = null!!

fun severalParamsInLambda(c: @Suspend() ((String, Int) -> Unit)) {}

fun foo() {
    builder({ 1 })
    builder { 1 }

    val x = { 1 }
    builder(x)
    builder({1} <!USELESS_CAST!>as (@Suspend() (() -> Int))<!>)

    var i: Int = 1
    i = genericBuilder({ 1 })
    i = genericBuilder { 1 }
    genericBuilder { 1 }
    genericBuilder<Int> { 1 }
    genericBuilder<Int> { <!TYPE_MISMATCH!>""<!> }

    val y = { 1 }
    genericBuilder(y)

    unitBuilder {}
    unitBuilder { <!UNUSED_EXPRESSION!>1<!> }
    unitBuilder({})
    unitBuilder({ <!UNUSED_EXPRESSION!>1<!> })

    manyArgumentsBuilder({}, { "" }) { 1 }

    val s: String = manyArgumentsBuilder({}, { "" }) { 1 }

    manyArgumentsBuilder<String>({}, { "" }, { 1 })
    manyArgumentsBuilder<String>({}, { <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> }, { 2 })

    severalParamsInLambda { x, y ->
        x checkType { _<String>() }
        y checkType { _<Int>() }
    }
}
