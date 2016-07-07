fun foo() {
    val
    println("abc")

    val
    lambdaCall {

    }

    val
    if (1 == 1) {

    }

    val
    (1 + 2)

    // `propertyNameOnTheNextLine` parsed as simple name expression
    val
    propertyNameOnTheNextLine

    val
    // comment
    propertyNameOnTheNextLine

    val /* comment */
    propertyNameOnTheNextLine

    // Correct properties
    val
    property1 = 1

    val
    propertyWithBy by lazy { 1 }

    val
    propertyWithType: Int

    val
    (a, b) = 1
}
