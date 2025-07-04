// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// KT-282 Nullability in extension functions and in binary calls

class Set {
    operator fun contains(x : Int) : Boolean = true
}

operator fun Set?.plus(x : Int) : Int = 1

operator fun Int?.contains(x : Int) : Boolean = false

fun f(): Unit {
    var set : Set? = null
    val i : Int? = null
    i <!UNSAFE_OPERATOR_CALL!>+<!> 1
    set + 1
    1 <!UNSAFE_OPERATOR_CALL!>in<!> set
    1 in 2
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
integerLiteral, localProperty, nullableType, operator, propertyDeclaration */
