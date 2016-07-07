// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun println(x: String) {
}

fun run(block: () -> Unit) {}

val propertyNameOnTheNextLine = 1

fun foo() {
    val<!SYNTAX!><!>
    println("abc")

    val<!SYNTAX!><!>
    run {
        println("abc")
    }

    val<!SYNTAX!><!>
    if (1 == 1) {

    }

    val<!SYNTAX!><!>
    (1 + 2)

    // `propertyNameOnTheNextLine` parsed as simple name expression
    val<!SYNTAX!><!>
    propertyNameOnTheNextLine

    val<!SYNTAX!><!>
    // comment
    propertyNameOnTheNextLine

    val<!SYNTAX!><!> /* comment */
    propertyNameOnTheNextLine

    // Correct properties
    val
    property1 = 1

    val
    <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>propertyWithBy<!> <!LOCAL_VARIABLE_WITH_DELEGATE!>by <!UNRESOLVED_REFERENCE!>lazy<!> { 1 }<!>

    val
    propertyWithType: Int

    val
    (a, b) = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>1<!>
}
