// !DIAGNOSTICS: -UNREACHABLE_CODE

fun none() {}

fun unitEmptyInfer() {}
fun unitEmpty() : Unit {}
fun unitEmptyReturn() : Unit {return}
fun unitIntReturn() : Unit {return <!RETURN_TYPE_MISMATCH!>1<!>}
fun unitUnitReturn() : Unit {return Unit}
fun test1() : Any = {<!RETURN_NOT_ALLOWED!>return<!>}
fun test2() : Any = a@ {return@a 1}
fun test3() : Any { return }
fun test4(): ()-> Unit = { <!RETURN_NOT_ALLOWED!>return@test4<!> }
fun test5(): Any = l@{ return@l }
fun test6(): Any = {<!RETURN_NOT_ALLOWED!>return<!> 1}

fun bbb() {
    return <!RETURN_TYPE_MISMATCH!>1<!>
}

fun foo(expr: StringBuilder): Int {
    val c = 'a'
    when(c) {
        0.toChar() -> throw Exception("zero")
        else -> throw Exception("nonzero" + c)
    }
}


fun unitShort() : Unit = Unit
fun unitShortConv() : Unit = <!RETURN_TYPE_MISMATCH!>1<!>
fun unitShortNull() : Unit = <!NULL_FOR_NONNULL_TYPE!>null<!>

fun intEmpty() : Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun intShortInfer() = 1
fun intShort() : Int = 1
//fun intBlockInfer()  {1}
fun intBlock() : Int {return 1}
fun intBlock1() : Int {1<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun intString(): Int = <!RETURN_TYPE_MISMATCH!>"s"<!>
fun intFunctionLiteral(): Int = <!RETURN_TYPE_MISMATCH!>{ 10 }<!>

fun blockReturnUnitMismatch() : Int {<!RETURN_TYPE_MISMATCH!>return<!>}
fun blockReturnValueTypeMismatch() : Int {return <!RETURN_TYPE_MISMATCH!>3.4<!>}
fun blockReturnValueTypeMatch() : Int {return 1}
fun blockReturnValueTypeMismatchUnit() : Int {return <!RETURN_TYPE_MISMATCH!>Unit<!>}

fun blockAndAndMismatch() : Int {
    true && false
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockAndAndMismatch1() : Int {
    return <!RETURN_TYPE_MISMATCH!>true && false<!>
}
fun blockAndAndMismatch2() : Int {
    (return <!RETURN_TYPE_MISMATCH!>true<!>) && (return <!RETURN_TYPE_MISMATCH!>false<!>)
}

fun blockAndAndMismatch3() : Int {
    true || false
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockAndAndMismatch4() : Int {
    return <!RETURN_TYPE_MISMATCH!>true || false<!>
}
fun blockAndAndMismatch5() : Int {
    (return <!RETURN_TYPE_MISMATCH!>true<!>) || (return <!RETURN_TYPE_MISMATCH!>false<!>)
}
fun blockReturnValueTypeMatch1() : Int {
    return <!RETURN_TYPE_MISMATCH!>if (1 > 2) 1.0 else 2.0<!>
}
fun blockReturnValueTypeMatch2() : Int {
    return <!INVALID_IF_AS_EXPRESSION!>if<!> (1 > 2) 1
}
fun blockReturnValueTypeMatch3() : Int {
    return <!RETURN_TYPE_MISMATCH!>if (1 > 2) else 1<!>
}
fun blockReturnValueTypeMatch4() : Int {
    if (1 > 2)
        return <!RETURN_TYPE_MISMATCH!>1.0<!>
    else return <!RETURN_TYPE_MISMATCH!>2.0<!>
}
fun blockReturnValueTypeMatch5() : Int {
    if (1 > 2)
        return <!RETURN_TYPE_MISMATCH!>1.0<!>
    return <!RETURN_TYPE_MISMATCH!>2.0<!>
}
fun blockReturnValueTypeMatch6() : Int {
    if (1 > 2)
    else return <!RETURN_TYPE_MISMATCH!>1.0<!>
    return <!RETURN_TYPE_MISMATCH!>2.0<!>
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
    return <!INVALID_IF_AS_EXPRESSION!>if<!> (1 > 2)
    1
}
fun blockReturnValueTypeMatch11() : Int {
    if (1 > 2)
    else 1.0
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun blockReturnValueTypeMatch12() : Int {
    if (1 > 2)
        return 1
    else return <!RETURN_TYPE_MISMATCH!>1.0<!>
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
fun nonBlockReturnIfEmptyIf(): Int = <!RETURN_TYPE_MISMATCH!>if (1 < 2) {} else {}<!>
fun nonBlockNoReturnIfUnitInOneBranch(): Int = <!RETURN_TYPE_MISMATCH!>if (1 < 2) {} else 2<!>

val a = <!RETURN_NOT_ALLOWED!>return<!> 1

class A() {
}
fun illegalConstantBody(): Int = <!RETURN_TYPE_MISMATCH!>"s"<!>
fun illegalConstantBlock(): String {
    return <!RETURN_TYPE_MISMATCH!>1<!>
}
fun illegalIfBody(): Int =
        <!RETURN_TYPE_MISMATCH!>if (1 < 2) 'a' else { 1.0 }<!>
fun illegalIfBlock(): Boolean {
    if (1 < 2)
        return false
    else { return <!RETURN_TYPE_MISMATCH!>1<!> }
}
fun illegalReturnIf(): Char {
    return if (1 < 2) 'a' else { 1 }
}

fun returnNothing(): Nothing {
    throw 1
}
fun f(): Int {
    if (1 < 2) { return 1 } else returnNothing()
}

fun f1(): Int = if (1 < 2) 1 else returnNothing()

public fun f2() = 1
class B() {
    protected fun f() = "ss"
}

fun testFunctionLiterals() {
    val endsWithVarDeclaration : () -> Boolean = <!INITIALIZER_TYPE_MISMATCH!>{
        val x = 2
    }<!>

    val endsWithAssignment: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>{
        var x = 1
        x = 333
    }<!>

    val endsWithReAssignment: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>{
        var x = 1
        x += 333
    }<!>

    val endsWithFunDeclaration : () -> String = <!INITIALIZER_TYPE_MISMATCH!>{
        var x = 1
        x = 333
        fun meow() : Unit {}
    }<!>

    val endsWithObjectDeclaration : () -> Int = <!INITIALIZER_TYPE_MISMATCH!>{
        var x = 1
        x = 333
        <!LOCAL_OBJECT_NOT_ALLOWED!>object A<!> {}
    }<!>

    val expectedUnitReturnType1: () -> Unit = {
        val x = 1
    }

    val expectedUnitReturnType2: () -> Unit = {
        fun meow() : Unit {}
        <!LOCAL_OBJECT_NOT_ALLOWED!>object A<!> {}
    }

}
