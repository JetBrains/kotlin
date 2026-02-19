// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB


class MyList {
    companion object {
        operator fun of(vararg elems: Int) = MyList()
    }
}

fun <T> nullable(): T? = null
fun <T: Any> takeNotNull(x: T) { }

fun smartcastFromCollectionLiteralArgument() {
    val x: Int? = nullable()
    val lit: MyList = [if (x != null) x else null!!, x]
    takeNotNull(x)
}

fun initializationInCollectionLiteralArgument() {
    val x: Boolean
    val lit: MyList = [if (true) {
        x = false
        0
    } else {
        x = true
        1
    }, if (x) 0 else 1]
    takeNotNull(x)
}

fun doubleInitializationInCollectionLiteralArguments() {
    val x: Boolean
    val lit: MyList = [
        if (true) { x = false; 0 } else { x = true; 1 },
        if (false) { <!VAL_REASSIGNMENT!>x<!> = true; 1 } else { <!VAL_REASSIGNMENT!>x<!> = false; 0 },
    ]
    takeNotNull(x)
}

fun initializationInRunInCollectionLiteral() {
    val x: Boolean
    val lit: MyList = [run { x = true; 0}]
    takeNotNull(x)
}

fun doubleInitializationInRunInCollectionLiteral() {
    val x: Boolean
    val lit: MyList = [run { x = true; 0}, run { <!VAL_REASSIGNMENT!>x<!> = false; 1 }]
    takeNotNull(x)
}

fun doubleVarAssignmentInRunInCollectionLiteral() {
    var x: Boolean
    val lit: MyList = [run { x = true; 0 }, run { x = false; 1 }]
    takeNotNull(x)
}

fun castInCollectionLiteral() {
    val x: Any? = nullable()
    val lit: MyList = [x as Int, x]
    takeNotNull(x)
}

/* GENERATED_FIR_TAGS: asExpression, assignment, checkNotNullCall, classDeclaration, companionObject, equalityExpression,
functionDeclaration, ifExpression, integerLiteral, lambdaLiteral, localProperty, nullableType, objectDeclaration,
operator, propertyDeclaration, smartcast, typeConstraint, typeParameter, vararg */
