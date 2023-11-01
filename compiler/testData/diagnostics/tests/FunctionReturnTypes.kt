// !DIAGNOSTICS: -UNREACHABLE_CODE

fun none() {}

fun unitEmptyInfer() {}
fun unitEmpty() : Unit {}
fun unitEmptyReturn() : Unit {return}
fun unitIntReturn() : Unit {return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>}
fun unitUnitReturn() : Unit {return Unit}
fun test1() : Any = {<!RETURN_NOT_ALLOWED, RETURN_TYPE_MISMATCH!>return<!>}
fun test2() : Any = a@ {return@a 1}
fun test3() : Any { <!RETURN_TYPE_MISMATCH!>return<!> }
fun test4(): ()-> Unit = { <!RETURN_NOT_ALLOWED, RETURN_TYPE_MISMATCH!>return@test4<!> }
fun test5(): Any = l@{ return@l }
fun test6(): Any = {<!RETURN_NOT_ALLOWED!>return<!> 1}

fun bbb() {
    return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
}

fun foo(expr: StringBuilder): Int {
    val c = 'a'
    when(c) {
        0.toChar() -> throw Exception("zero")
        else -> throw Exception("nonzero" + c)
    }
}


fun unitShort() : Unit = Unit
fun unitShortConv() : Unit = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
fun unitShortNull() : Unit = <!NULL_FOR_NONNULL_TYPE!>null<!>

fun intEmpty() : Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun intShortInfer() = 1
fun intShort() : Int = 1
//fun intBlockInfer()  {1}
fun intBlock() : Int {return 1}
fun intBlock1() : Int {1<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun intString(): Int = <!TYPE_MISMATCH!>"s"<!>
fun intFunctionLiteral(): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ 10 }<!>

fun blockReturnUnitMismatch() : Int {<!RETURN_TYPE_MISMATCH!>return<!>}
fun blockReturnValueTypeMismatch() : Int {return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>3.4<!>}
fun blockReturnValueTypeMatch() : Int {return 1}
fun blockReturnValueTypeMismatchUnit() : Int {return <!TYPE_MISMATCH!>Unit<!>}

fun blockAndAndMismatch() : Int {
    true && false
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockAndAndMismatch1() : Int {
    return <!TYPE_MISMATCH!>true && false<!>
}
fun blockAndAndMismatch2() : Int {
    (return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>) && (return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>false<!>)
}

fun blockAndAndMismatch3() : Int {
    true || false
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockAndAndMismatch4() : Int {
    return <!TYPE_MISMATCH!>true || false<!>
}
fun blockAndAndMismatch5() : Int {
    (return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>) || (return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>false<!>)
}
fun blockReturnValueTypeMatch1() : Int {
    return if (1 > 2) <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1.0<!> else <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2.0<!>
}
fun blockReturnValueTypeMatch2() : Int {
    return <!TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (1 > 2) 1<!>
}
fun blockReturnValueTypeMatch3() : Int {
    return <!TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (1 > 2) else 1<!>
}
fun blockReturnValueTypeMatch4() : Int {
    if (1 > 2)
        return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1.0<!>
    else return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2.0<!>
}
fun blockReturnValueTypeMatch5() : Int {
    if (1 > 2)
        return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1.0<!>
    return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2.0<!>
}
fun blockReturnValueTypeMatch6() : Int {
    if (1 > 2)
    else return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1.0<!>
    return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2.0<!>
}
fun blockReturnValueTypeMatch7() : Int {
    if (1 > 2)
    1.0
    else 2.0
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockReturnValueTypeMatch8() : Int {
    if (1 > 2)
    1.0
    else 2.0
    return 1
}
fun blockReturnValueTypeMatch9() : Int {
    if (1 > 2)
    1.0
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockReturnValueTypeMatch10() : Int {
    return <!TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (1 > 2)
    1<!>
}
fun blockReturnValueTypeMatch11() : Int {
    if (1 > 2)
    else 1.0
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockReturnValueTypeMatch12() : Int {
    if (1 > 2)
        return 1
    else return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1.0<!>
}
fun blockNoReturnIfValDeclaration(): Int {
    val x = 1
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockNoReturnIfEmptyIf(): Int {
    if (1 < 2) {} else {}
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockNoReturnIfUnitInOneBranch(): Int {
    if (1 < 2) {
        return 1
    } else {
        if (3 < 4) {
        } else {
            return 2
        }
    }
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun nonBlockReturnIfEmptyIf(): Int = if (1 < 2) <!TYPE_MISMATCH!>{}<!> else <!TYPE_MISMATCH!>{}<!>
fun nonBlockNoReturnIfUnitInOneBranch(): Int = if (1 < 2) <!TYPE_MISMATCH!>{}<!> else 2

val a = <!RETURN_NOT_ALLOWED!>return<!> 1

class A() {
}
fun illegalConstantBody(): Int = <!TYPE_MISMATCH!>"s"<!>
fun illegalConstantBlock(): String {
    return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
}
fun illegalIfBody(): Int =
        if (1 < 2) <!CONSTANT_EXPECTED_TYPE_MISMATCH!>'a'<!> else <!TYPE_MISMATCH!>{ 1.0 }<!>
fun illegalIfBlock(): Boolean {
    if (1 < 2)
        return false
    else { return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> }
}
fun illegalReturnIf(): Char {
    return if (1 < 2) 'a' else <!TYPE_MISMATCH!>{ 1 }<!>
}

fun returnNothing(): Nothing {
    throw <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
}
fun returnNothingEmpty(): Nothing {
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun f(): Int {
    if (1 < 2) { return 1 } else returnNothing()
}

fun f1(): Int = if (1 < 2) 1 else returnNothing()

public fun f2() = 1
class B() {
    protected fun f() = "ss"
}

fun testFunctionLiterals() {
    val endsWithVarDeclaration : () -> Boolean = {
        <!EXPECTED_TYPE_MISMATCH!>val x = 2<!>
    }

    val endsWithAssignment: () -> Int = {
        var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>x<!> = 1
        <!EXPECTED_TYPE_MISMATCH!>x = 333<!>
    }

    val endsWithReAssignment: () -> Int = {
        var x = 1
        <!ASSIGNMENT_TYPE_MISMATCH!>x += 333<!>
    }

    val endsWithFunDeclaration : () -> String = {
        var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>x<!> = 1
        x = 333
        <!EXPECTED_TYPE_MISMATCH!>fun meow() : Unit {}<!>
    }

    val endsWithObjectDeclaration : () -> Int = {
        var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>x<!> = 1
        x = 333
        <!EXPECTED_TYPE_MISMATCH, LOCAL_OBJECT_NOT_ALLOWED!>object A<!> {}
    }

    val expectedUnitReturnType1: () -> Unit = {
        val x = 1
    }

    val expectedUnitReturnType2: () -> Unit = {
        fun meow() : Unit {}
        <!LOCAL_OBJECT_NOT_ALLOWED!>object A<!> {}
    }

}
