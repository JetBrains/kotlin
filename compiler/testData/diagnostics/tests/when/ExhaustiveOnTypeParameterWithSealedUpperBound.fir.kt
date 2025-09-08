// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ImprovedExhaustivenessChecksIn21
sealed class Bird

class Penguin : Bird()
class Ostrich : Bird()
class Kiwi : Bird()

sealed class Vehicle

class Car : Vehicle()
class Motocycle : Vehicle()

interface I

fun <T : Bird> simple(value: T) {
    val v = when (value) {
        is Penguin -> "Snow sledding on your belly sounds fun"
        is Ostrich -> "ostentatious and rich"
        is Kiwi -> "kiwiwiwiwi"
    }
}

fun <T> oneSealedOneUnrelated(value: T) where T : Bird, T : I {
    val v = when (value) {
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Penguin<!> -> "Snow sledding on your belly sounds fun"
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Ostrich<!> -> "ostentatious and rich"
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Kiwi<!> -> "kiwiwiwiwi"
    }
}

fun <T> twoSealed(value: T) where T : Bird, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>Vehicle<!> {
    val v = when (value) {
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Penguin<!> -> "Snow sledding on your belly sounds fun"
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Ostrich<!> -> "ostentatious and rich"
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Kiwi<!> -> "kiwiwiwiwi"
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, isExpression, localProperty,
propertyDeclaration, sealed, smartcast, stringLiteral, typeConstraint, typeParameter, whenExpression, whenWithSubject */
