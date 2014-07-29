// !DIAGNOSTICS: -UNREACHABLE_CODE
package kt770_351_735


//KT-770 Reference is not resolved to anything, but is not marked unresolved
fun main(args : Array<String>) {
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
    val r = {  // type fun(): Int is inferred
        if (true) {
            2
        }
        else {
            z = 34
        }
    }
    val <!UNUSED_VARIABLE!>f<!>: ()-> Int = <!TYPE_MISMATCH!>r<!>
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
    val <!UNUSED_VARIABLE!>f<!> : () -> String = <!TYPE_MISMATCH!>checkType<!>
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

    var <!UNUSED_VARIABLE!>u<!> = <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>when(d) {
        3 -> {
            z = <!UNUSED_VALUE!>34<!>
        }
        else -> <!UNUSED_CHANGED_VALUE!>z--<!>
    }<!>

    var <!UNUSED_VARIABLE!>iff<!> = <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>if (true) {
        z = <!UNUSED_VALUE!>34<!>
    }<!>
    val <!UNUSED_VARIABLE!>g<!> = <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>if (true) 4<!>
    val <!UNUSED_VARIABLE!>h<!> = <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>if (false) 4 else {}<!>

    bar(if (true) {
        <!CONSTANT_EXPECTED_TYPE_MISMATCH!>4<!>
    }
    else {
        z = <!UNUSED_VALUE!>342<!>
    })
}

fun bar(<!UNUSED_PARAMETER!>a<!>: Unit) {}

fun testStatementInExpressionContext() {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>z<!> = 34
    val <!UNUSED_VARIABLE!>a1<!>: Unit = <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>z = <!UNUSED_VALUE!>334<!><!>
    val <!UNUSED_VARIABLE!>f<!> = <!EXPRESSION_EXPECTED!>for (i in 1..10) {}<!>
    if (true) return <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>z = <!UNUSED_VALUE!>34<!><!>
    return <!EXPRESSION_EXPECTED!>while (true) {}<!>
}

fun testStatementInExpressionContext2() {
    val <!UNUSED_VARIABLE!>a2<!>: Unit = <!EXPRESSION_EXPECTED!>while(true) {}<!>
}