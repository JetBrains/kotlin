// INTENTION_TEXT: "Import members from 'JavaUtilClass'"
// WITH_RUNTIME
// ERROR: None of the following functions can be called with the arguments supplied: <br>public open fun overloadedMethod(i: Int): Unit defined in JavaUtilClass<br>public open fun overloadedMethod(i: String!): Unit defined in JavaUtilClass
// ERROR: None of the following functions can be called with the arguments supplied: <br>public open fun overloadedMethod(i: Int): Unit defined in JavaUtilClass<br>public open fun overloadedMethod(i: String!): Unit defined in JavaUtilClass
// ERROR: Unresolved reference: unresolved

fun foo() {
    <caret>JavaUtilClass.overloadedMethod()

    val bottom = JavaUtilClass.STATIC_FIELD

    JavaUtilClass.overloadedMethod()

    JavaUtilClass.unresolved
}
