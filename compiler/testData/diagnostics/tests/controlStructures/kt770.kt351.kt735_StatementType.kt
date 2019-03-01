// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNREACHABLE_CODE
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
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>z<!> = 2
    val r = {  // type fun(): Any is inferred
        if (true) {
            2
        }
        else {
            z = 34
        }
    }
    val <!UNUSED_VARIABLE!>f<!>: ()-> Int = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>r<!>
    val <!UNUSED_VARIABLE!>g<!>: ()-> Any = r
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
    val <!UNUSED_VARIABLE!>simple<!>: ()-> Unit = {
        <!UNUSED_EXPRESSION!>41<!>
    }
    val <!UNUSED_VARIABLE!>withIf<!>: ()-> Unit = {
        if (true) {
            <!UNUSED_EXPRESSION!>3<!>
        } else {
            <!UNUSED_EXPRESSION!>45<!>
        }
    }
    val i = 34
    val <!UNUSED_VARIABLE!>withWhen<!> : () -> Unit = {
        when(i) {
            1 -> {
                val d = 34
                <!UNUSED_EXPRESSION!>"1"<!>
                doSmth(d)

            }
            2 -> <!UNUSED_EXPRESSION!>'4'<!>
            else -> <!UNUSED_EXPRESSION!>true<!>
        }
    }

    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>x<!> = 43
    val checkType = {
        if (true) {
            x = 4
        } else {
            45
        }
    }
    val <!UNUSED_VARIABLE!>f<!> : () -> String = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>checkType<!>
}

fun doSmth(<!UNUSED_PARAMETER!>i<!>: Int) {}

fun testImplicitCoercion() {
    val d = 21
    var z = 0
    var <!UNUSED_VARIABLE!>i<!> = when(d) {
        3 -> null
        4 -> { val <!NAME_SHADOWING, UNUSED_VARIABLE!>z<!> = 23 }
        else -> z = 20
    }

    var <!UNUSED_VARIABLE!>u<!> = when(d) {
        3 -> {
        <!IMPLICIT_CAST_TO_ANY!><!UNUSED_VALUE!>z =<!> 34<!>
    }
        else -> <!IMPLICIT_CAST_TO_ANY, UNUSED_CHANGED_VALUE!>z--<!>
    }

    var <!UNUSED_VARIABLE!>iff<!> = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) {
        <!UNUSED_VALUE!>z =<!> 34
    }
    val <!UNUSED_VARIABLE!>g<!> = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 4
    val <!UNUSED_VARIABLE!>h<!> = if (false) <!IMPLICIT_CAST_TO_ANY!>4<!> else <!IMPLICIT_CAST_TO_ANY!>{}<!>

    bar(<!NI;TYPE_MISMATCH!>if (true) {
        <!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>4<!>
    }
        else {
        <!UNUSED_VALUE!>z =<!> 342
    }<!>)
}

fun fooWithAnyArg(<!UNUSED_PARAMETER!>arg<!>: Any) {}
fun fooWithAnyNullableArg(<!UNUSED_PARAMETER!>arg<!>: Any?) {}

fun testCoercionToAny() {
    val d = 21
    val <!UNUSED_VARIABLE!>x1<!>: Any = if (1>2) 1 else 2.0
    val <!UNUSED_VARIABLE!>x2<!>: Any? = if (1>2) 1 else 2.0
    val <!UNUSED_VARIABLE!>x3<!>: Any? = if (1>2) 1 else (if (1>2) null else 2.0)

    fooWithAnyArg(if (1>2) 1 else 2.0)
    fooWithAnyNullableArg(if (1>2) 1 else 2.0)
    fooWithAnyNullableArg(if (1>2) 1 else (if (1>2) null else 2.0))

    val <!UNUSED_VARIABLE!>y1<!>: Any = when(d) { 1 -> 1.0 else -> 2.0 }
    val <!UNUSED_VARIABLE!>y2<!>: Any? = when(d) { 1 -> 1.0 else -> 2.0 }
    val <!UNUSED_VARIABLE!>y3<!>: Any? = when(d) { 1 -> 1.0; 2 -> null; else -> 2.0 }

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

fun bar(<!UNUSED_PARAMETER!>a<!>: Unit) {}

fun testStatementInExpressionContext() {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>z<!> = 34
    val <!UNUSED_VARIABLE!>a1<!>: Unit = <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!><!UNUSED_VALUE!>z =<!> 334<!>
    val <!UNUSED_VARIABLE!>f<!> = <!EXPRESSION_EXPECTED!>for (i in 1..10) {}<!>
    if (true) return <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!><!UNUSED_VALUE!>z =<!> 34<!>
    return <!EXPRESSION_EXPECTED!>while (true) {}<!>
}

fun testStatementInExpressionContext2() {
    val <!UNUSED_VARIABLE!>a2<!>: Unit = <!EXPRESSION_EXPECTED!>while(true) {}<!>
}