// RUN_PIPELINE_TILL: FRONTEND

fun test(): Int {
    var x: String? = "!"
    val arr = Array(42) { x = null }
    return if (arr.size == 42) {
        x = "!"
        x.length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

fun doubleTest(): Int {
    var x: String? = "!"
    val arr = DoubleArray(42) { x = null; 42.0 }
    return if (arr.size == 42) {
        x = "!"
        x.length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

typealias MyDoubleArray = Array<Double>

fun myDoubleTest(): Int {
    var x: String? = "!"
    val arr = MyDoubleArray(42) { x = null; 42.0 }
    return if (arr.size == 42) {
        x = "!"
        x.length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, incrementDecrementExpression,
integerLiteral, intersectionType, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral */
