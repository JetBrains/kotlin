@CompileTimeCalculation
class A

@CompileTimeCalculation
fun notNullAssertion(value: Int?): String {
    return try {
        value!!
        "Value isn't null"
    } catch (e: NullPointerException) {
        "Value is null"
    }
}

@CompileTimeCalculation
fun notNullAssertionForObject(value: A?): String {
    return try {
        value!!
        "Value isn't null"
    } catch (e: NullPointerException) {
        "Value is null"
    }
}

@CompileTimeCalculation
fun notNullAssertionForSomeWrapper(value: StringBuilder?): String {
    return try {
        value!!.toString()
    } catch (e: NullPointerException) {
        "Value is null"
    }
}

@CompileTimeCalculation
fun notNullLambda(lambda: (() -> String)?): String {
    return when {
        lambda != null -> lambda()
        else -> "Lambda is null"
    }
}

@CompileTimeCalculation
fun nullableCast(str: String?): String {
    return try {
        str as String
    } catch (e: NullPointerException) {
        "Null"
    }
}

const val a1 = <!EVALUATED: `Value isn't null`!>notNullAssertion(1)<!>
const val a2 = <!EVALUATED: `Value is null`!>notNullAssertion(null)<!>
const val b1 = <!EVALUATED: `Value isn't null`!>notNullAssertionForObject(A())<!>
const val b2 = <!EVALUATED: `Value is null`!>notNullAssertionForObject(null)<!>
const val c1 = <!EVALUATED: `Some text`!>notNullAssertionForSomeWrapper(StringBuilder("Some text"))<!>
const val c2 = <!EVALUATED: `Value is null`!>notNullAssertionForSomeWrapper(null)<!>
const val d1 = <!EVALUATED: `Not null lambda`!>notNullLambda { "Not null lambda" }<!>
const val d2 = <!EVALUATED: `Lambda is null`!>notNullLambda(null)<!>
const val e1 = <!EVALUATED: `Not null String`!>nullableCast("Not null String")<!>
const val e2 = <!EVALUATED: `Null`!>nullableCast(null)<!>
