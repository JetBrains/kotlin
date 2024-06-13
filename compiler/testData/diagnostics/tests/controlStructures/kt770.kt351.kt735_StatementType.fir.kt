// DIAGNOSTICS: -UNREACHABLE_CODE
package kt770_351_735


//KT-770 Reference is not resolved to anything, but is not marked unresolved
fun main() {
    var i = 0
    when (i) {
        1 -> i--
        2 -> i = 2  // i is surrounded by a black border
        else -> <!UNRESOLVED_REFERENCE!>j<!> = 2
    }
    System.out.println(i)
}

//KT-351 Distinguish statement and expression positions
val w = <!EXPRESSION_EXPECTED!>while (true) {}<!>

fun foo() {
    var z = 2
    val r = {  // type fun(): Any is inferred
        if (true) {
            2
        }
        else {
            z = 34
        }
    }
    val f: ()-> Int = <!INITIALIZER_TYPE_MISMATCH!>r<!>
    val g: ()-> Any = r
}

//KT-735 Statements without braces are prohibited on the right side of when entries.
fun box() : Int {
    val d = 2
    var z = 0
    when(d) {
        5, 3 -> z++
        else -> z = -1000
    }
    return z
}

//More tests

fun test1() { while(true) {} }
fun test2(): Unit { while(true) {} }

fun testCoercionToUnit() {
    val simple: ()-> Unit = {
        41
    }
    val withIf: ()-> Unit = {
        if (true) {
            3
        } else {
            45
        }
    }
    val i = 34
    val withWhen : () -> Unit = {
        when(i) {
            1 -> {
                val d = 34
                "1"
                doSmth(d)

            }
            2 -> '4'
            else -> true
        }
    }

    var x = 43
    val checkType = {
        if (true) {
            x = 4
        } else {
            45
        }
    }
    val f : () -> String = <!INITIALIZER_TYPE_MISMATCH!>checkType<!>
}

fun doSmth(i: Int) {}

fun testImplicitCoercion() {
    val d = 21
    var z = 0
    var i = when(d) {
        3 -> null
        4 -> { val z = 23 }
        else -> z = 20
    }

    var u = when(d) {
        3 -> { z = 34 }
        else -> z--
    }

    var iff = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) {
        z = 34
    }
    val g = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 4
    val h = if (false) 4 else {}

    bar(<!ARGUMENT_TYPE_MISMATCH!>if (true) {
        4
    } else {
        z = 342
    }<!>)
}

fun fooWithAnyArg(arg: Any) {}
fun fooWithAnyNullableArg(arg: Any?) {}

fun testCoercionToAny() {
    val d = 21
    val x1: Any = if (1>2) 1 else 2.0
    val x2: Any? = if (1>2) 1 else 2.0
    val x3: Any? = if (1>2) 1 else (if (1>2) null else 2.0)

    fooWithAnyArg(if (1>2) 1 else 2.0)
    fooWithAnyNullableArg(if (1>2) 1 else 2.0)
    fooWithAnyNullableArg(if (1>2) 1 else (if (1>2) null else 2.0))

    val y1: Any = when(d) { 1 -> 1.0 else -> 2.0 }
    val y2: Any? = when(d) { 1 -> 1.0 else -> 2.0 }
    val y3: Any? = when(d) { 1 -> 1.0; 2 -> null; else -> 2.0 }

    fooWithAnyArg(when(d) { 1 -> 1.0 else -> 2.0 })
    fooWithAnyNullableArg(when(d) { 1 -> 1.0 else -> 2.0 })
    fooWithAnyNullableArg(when(d) { 1 -> 1.0; 2 -> null; else -> 2.0 })
}

fun fooWithAnuNullableResult(s: String?, name: String, optional: Boolean): Any? {
    return if (s == null) {
        if (!optional) {
            throw java.lang.IllegalArgumentException("Parameter '$name' was not found in the request")
        }
        null
    } else {
        name
    }
}

fun bar(a: Unit) {}

fun testStatementInExpressionContext() {
    var z = 34
    val a1: Unit = <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>z = 334<!>
    val f = <!EXPRESSION_EXPECTED!>for (i in 1..10) {}<!>
    if (true) return <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>z = 34<!>
    return <!EXPRESSION_EXPECTED!>while (true) {}<!>
}

fun testStatementInExpressionContext2() {
    val a2: Unit = <!EXPRESSION_EXPECTED!>while(true) {}<!>
}
