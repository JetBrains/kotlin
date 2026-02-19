// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun simpleWhile(x: Int?, y0: Int) {
    var y = y0
    while (x!! == y) {
        checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
        y++
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}

fun whileWithBreak(x: Int?, y0: Int) {
    var y = y0
    while (x!! == y) {
        checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
        break
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}

fun whileWithNoCondition(x: Int?) {
    while (<!SYNTAX!><!>) {
        x!!
    }
    checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
}

/* GENERATED_FIR_TAGS: assignment, break, checkNotNullCall, classDeclaration, equalityExpression,
funWithExtensionReceiver, functionDeclaration, functionalType, incrementDecrementExpression, infix, localProperty,
nullableType, propertyDeclaration, smartcast, typeParameter, typeWithExtension, whileLoop */
