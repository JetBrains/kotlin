// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun statementWhenWithElse() {
    val testParam = "testValue"
    when (testParam) {
        "testValue" -> print("Found")
        "anotherValue" -> print("Found another value")
        else -> print("Not Found")
    }
}

fun statementWhenWithoutElse() {
    val testParam = "testValue"
    when (testParam) {
        "testValue" -> print("Found")
        "anotherValue" -> print("Found another value")
    }
}

fun expressionWhenWithElse() {
    val testParam = "testValue"
    val result = when (testParam) {
        "testValue" -> "Found"
        "anotherValue" -> "Found another value"
        else -> "Nothing"
    }
}

fun expressionWhenWithoutElse() {
    val testParam = "testValue"
    val result = <!NO_ELSE_IN_WHEN!>when<!> (testParam) {
        "testValue" -> "Found"
        "anotherValue" -> "Found another value"
    }
}

fun expressionWhenWithBoolean() {
    val flag = true
    val result = when (flag) {
        true -> "true"
        false -> "false"
    }
}

fun expressionWhenWithBooleanAndUnusedElse() {
    val flag = true
    val result = when (flag) {
        true -> "true"
        false -> "false"
            <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> "No use"
    }
}

fun expressionWhenWithIncompatibleType() {
    val flag = 1
    val result = <!NO_ELSE_IN_WHEN!>when<!> (flag) {
        <!INCOMPATIBLE_TYPES!>true<!> -> "true"
        <!INCOMPATIBLE_TYPES!>false<!> -> "false"
    }
}

// Expression with Enums
enum class Fruite {
    APPLE, ORANGE
}

fun expressionWhenWithEnumPositive():String {
    val numericValue = when (Fruite.ORANGE) {
        Fruite.APPLE -> "apple"
        Fruite.ORANGE -> "orange"
    }
    return numericValue
}

fun expressionWhenWithEnumCoveringNotAllScenarios() {
    val numericValue = <!NO_ELSE_IN_WHEN!>when<!> (Fruite.ORANGE) {
        Fruite.APPLE -> "apple"
    }
}

fun expressionWhenWithEnumWithInvalidInputType() {
    val numericValue = <!NO_ELSE_IN_WHEN!>when<!> (1) {
        <!INCOMPATIBLE_TYPES!>Fruite.APPLE<!> -> "apple"
        <!INCOMPATIBLE_TYPES!>Fruite.ORANGE<!> -> "orange"
    }
}

fun expressionWhenWithEnumWithInvalidInputTypeAndElse() {
    val numericValue = when (1) {
            <!INCOMPATIBLE_TYPES!>Fruite.APPLE<!> -> "apple"
            <!INCOMPATIBLE_TYPES!>Fruite.ORANGE<!> -> "orange"
        else -> "Else"
    }
}

fun expressionWhenWithContextSensitiveResolution(fruiete: Fruite):String {
    val numericValue = <!NO_ELSE_IN_WHEN!>when<!> (fruiete) {
        <!UNRESOLVED_REFERENCE!>APPLE<!> -> "apple"
        <!UNRESOLVED_REFERENCE!>ORANGE<!> -> "orange"
    }
    return numericValue
}

// Sealed classes
sealed class Animal
class Dog : Animal()
class Cat : Animal()

fun expressionWhenWithSealedClass(animal: Animal): String {
    return when(animal) {
        is Dog -> "Dog"
        is Cat -> "Cat"
    }
}

fun expressionWhenWithTypeAsParameter(animal: Animal): String {
    return <!NO_ELSE_IN_WHEN!>when<!>(<!NO_COMPANION_OBJECT!>Animal<!>) {
        is Dog -> "Dog"
        is Cat -> "Cat"
    }
}

fun expressionWhenWithSealedClassAndPartOptionsIncluded(animal: Animal): String {
    return <!NO_ELSE_IN_WHEN!>when<!>(animal) {
        is Dog -> "Dog"
    }
}

fun expressionWhenWithSealedClassWithInvalidInputType(animal: String): String {
    return <!NO_ELSE_IN_WHEN!>when<!>(animal) {
        is <!INCOMPATIBLE_TYPES!>Dog<!> -> "Dog"
        is <!INCOMPATIBLE_TYPES!>Cat<!> -> "Cat"
    }
}

fun expressionWhenWithSealedClassWithInvalidInputTypeWithElse(animal: String): String {
    return when(animal) {
        is <!INCOMPATIBLE_TYPES!>Dog<!> -> "Dog"
        is <!INCOMPATIBLE_TYPES!>Cat<!> -> "Cat"
        else -> "String"
    }
}

open class AnotherAnimal
class AnotherDog : AnotherAnimal()
class AnotherCat : AnotherAnimal()

fun expressionWhenWithOpenClassWithElse(anotherAnimal: AnotherAnimal): String {
    return when(anotherAnimal) {
        is AnotherDog -> "AnotherDog"
        is AnotherCat -> "AnotherCat"
        else -> "Else!"
    }
}

fun expressionWhenWithOpenClassWithoutElse(anotherAnimal: AnotherAnimal): String {
    return <!NO_ELSE_IN_WHEN!>when<!>(anotherAnimal) {
        is AnotherDog -> "AnotherDog"
        is AnotherCat -> "AnotherCat"
    }
}

fun expressionWhenWithOpenClassWithElseAndInvalidInputType(anotherAnimal: String): String {
    return when(anotherAnimal) {
        is <!INCOMPATIBLE_TYPES!>AnotherDog<!> -> "AnotherDog"
        is <!INCOMPATIBLE_TYPES!>AnotherCat<!> -> "AnotherCat"
        else -> "Else!"
    }
}

fun expressionWhenWithoutParameterAndOptions() {
    val option = 1
    val result = when {
        option > 0 -> "Success"
        else -> "Else"
    }
}

fun expressionWhenWithoutParameterAndOptionsWithoutElse() {
    val option = 1
    val result = <!NO_ELSE_IN_WHEN!>when<!> {
        option > 0 -> "Success"
    }
}

// Mulltiple checks
fun statementWhenWithMulltipleCheckWithoutElse() {
    val searchFor = "Money"
    when (searchFor) {
        "Car", "Wathces", "House" -> print ("Not Money")
        "Money", "Dinnero" -> "Money!"
    }
}

fun statementWhenWithMulltipleCheckWithElse() {
    val searchFor = "Money"
    when (searchFor) {
        "Car", "Wathces", "House" -> print ("Not Money")
        "Money", "Dinnero" -> "Money!"
        else -> "Happy"
    }
}

fun expressionWhenWithMulltipleCheckWithElse() {
    val searchFor = "Money"
    val result = when (searchFor) {
        "Car", "Wathces", "House" -> <!IMPLICIT_CAST_TO_ANY!>print ("Not Money")<!>
            "Money", "Dinnero" -> <!IMPLICIT_CAST_TO_ANY!>"Money!"<!>
        else -> <!IMPLICIT_CAST_TO_ANY!>"Happy"<!>
    }
}

fun expressionWhenWithMulltipleCheckWithoutElse() {
    val searchFor = "Money"
    val result = <!NO_ELSE_IN_WHEN!>when<!> (searchFor) {
        "Car", "Wathces", "House" -> <!IMPLICIT_CAST_TO_ANY!>print ("Not Money")<!>
        "Money", "Dinnero" -> <!IMPLICIT_CAST_TO_ANY!>"Money!"<!>
    }
}

fun expressionWhenWithBooleanEvaluation() {
    val testValue = "1"

    val result = when (testValue) {
        1.toString() -> "One"
        else -> "None"
    }
}

fun expressionWhenWithBooleanEvaluationWithoutElse() {
    val testValue = "1"

    val result = <!NO_ELSE_IN_WHEN!>when<!> (testValue) {
        1.toString() -> "One"
    }
}

fun expressionWhenWithInvalidCast() {
    val testValue = "1"

    val result = when (testValue) {
        1 <!CAST_NEVER_SUCCEEDS!>as<!> String -> "One"
        else -> "None"
    }
}

fun expressionWhenWithRangeCheck() {
    val testValue = 1
    val testValues = setOf(11, 12, 13)

    val result = when(testValue) {
        in 1..10 -> "In 1 to 10"
        in testValues -> "From 11 to 13"
        !in 10..20 -> "Not between 10 and 20"
        else -> "Who knows"
    }
}

fun expressionWhenWithTypeCheck() {
    val testValue: String = "testValue"

    val result = when(testValue) {
            <!USELESS_IS_CHECK!>is String<!> -> "This is STRIIIIIIING!"
        else -> "Not String"
    }
}

fun expressionWhenWithIncorrectTypeCheck() {
    val testValue: String = "testValue"

    val result = when(testValue) {
        is<!SYNTAX!><!> -> "This is INTEGER!"
        else -> "String?"
    }
}

fun expressionWhenWithTypeCheckWithoutElse() {
    val testValue: String = "testValue"

    val result = <!NO_ELSE_IN_WHEN!>when<!>(testValue) {
        is <!INCOMPATIBLE_TYPES, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!> -> "This is STRIIIIIIING!"
    }
}


fun expressionWhenWithTypeCheckSmartCast(testValue: Any) {
    val result = when(testValue) {
        is String -> <!DEBUG_INFO_SMARTCAST!>testValue<!>.startsWith("test")
        else -> false
    }
}

fun expressionWhenWithValueCreatedInWhen() {
    val result = when (val testValue = true) {
        true -> 1
        false -> 2
    }
}

// Guard
fun guardExpression() {
    val firstTestValue = "1"
    val secondTestValue = true

    when(firstTestValue) {
        is <!INCOMPATIBLE_TYPES, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!> -> print("First value is Integer")
        is <!DUPLICATE_LABEL_IN_WHEN, INCOMPATIBLE_TYPES, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!> <!UNSUPPORTED_FEATURE!>if secondTestValue<!> -> print("Condition Failed")
            <!USELESS_IS_CHECK!>is String<!> <!UNSUPPORTED_FEATURE!>if secondTestValue<!> -> print("Condition Passed")
        else -> "What are we?"
    }
}

fun guardExpressionWithDoubleCheck() {
    val firstTestValue = "1"
    val secondTestValue = true
    val thirdTestValue = false

    when(firstTestValue) {
        is <!INCOMPATIBLE_TYPES, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!> -> print("First value is Integer")
        is <!DUPLICATE_LABEL_IN_WHEN, INCOMPATIBLE_TYPES, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!> <!UNSUPPORTED_FEATURE!>if secondTestValue<!> -> print("Condition Failed")
            <!USELESS_IS_CHECK!>is String<!> <!UNSUPPORTED_FEATURE!>if (secondTestValue && !thirdTestValue)<!> -> print("Condition Passed")
        else -> "What are we?"
    }
}

fun guardExpressionWithTwoOptionsCheck() {
    val firstTestValue = "1"
    val secondTestValue = false
    val thirdTestValue = true

    when(firstTestValue) {
        is <!INCOMPATIBLE_TYPES, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!> -> print("First value is Integer")
        is <!DUPLICATE_LABEL_IN_WHEN, INCOMPATIBLE_TYPES, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!> <!UNSUPPORTED_FEATURE!>if secondTestValue<!> -> print("Condition Failed")
            <!USELESS_IS_CHECK!>is String<!> <!UNSUPPORTED_FEATURE!>if secondTestValue || thirdTestValue<!> -> print("Condition Passed")
        else -> "What are we?"
    }
}

fun guardExpressionWithTwoOptionsCheckAndIfElse() {
    val firstTestValue = "1"
    val secondTestValue = false
    val thirdTestValue = false
    val fallbackValue = false

    when(firstTestValue) {
        is <!INCOMPATIBLE_TYPES, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!> -> print("First value is Integer")
        is <!DUPLICATE_LABEL_IN_WHEN, INCOMPATIBLE_TYPES, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!> <!UNSUPPORTED_FEATURE!>if secondTestValue<!> -> print("Condition Failed")
            <!USELESS_IS_CHECK!>is String<!> <!UNSUPPORTED_FEATURE!>if secondTestValue || thirdTestValue<!> -> print("Condition Failed")
        else <!UNSUPPORTED_FEATURE!>if (fallbackValue)<!> -> print("Condition Passed")
        else -> "What are we?"
    }
}

fun statementWhenWithNullAsValue() {
    val nullValue = null
    when(<!DEBUG_INFO_CONSTANT!>nullValue<!>) {
        <!USELESS_IS_CHECK!>is String<!> -> print("First value is String")
        else -> print("Else")
    }
}

fun expressionWhenWithBooleanAndCheckForNull(fruite: Fruite): String {
    val nullValue = null
    return when(<!DEBUG_INFO_CONSTANT!>nullValue<!>) {
        is <!SYNTAX!>null<!> -> "Fails"
        else -> "Else"
    }
}

fun statementWhenWithNothingAsValue() {
    val nullValue = <!NO_COMPANION_OBJECT!>Nothing<!>
    when(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>nullValue<!>) {
        is String -> print("First value is String")
        else -> print("Else")
    }
}

fun statementWhenWithVoidAsValue() {
    val nullValue = <!NO_COMPANION_OBJECT!>Void<!>
    when(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>nullValue<!>) {
        is String -> print("First value is String")
        else -> print("Else")
    }
}

class ClassWithWhenDeclarationInMethod() {
    fun baseWhenInClassMthod(): Boolean {
        return when (0) {
            0 -> true;
            else -> false;
        }
    }
}

class ClassWithInvalidWhenDeclaration() {
    <!SYNTAX!>when<!> <!SYNTAX!>(<!><!SYNTAX!>1<!><!SYNTAX!>)<!> <!SYNTAX!><!>{
        0 <!SYNTAX!>-> true<!><!SYNTAX!>;<!>
        <!SYNTAX!>else<!> <!SYNTAX!>-> false<!><!SYNTAX!>;<!>
    }
}

class ClassWithWhenFunctionInCompObj() {
    companion object {
        fun baseWhenInClassCompanionObjectMthod(): Boolean {
            return when (1) {
                0 -> true;
                else -> false;
            }
        }
    }
}

class ClassWithInvalidWhenDeclarationInCompObj() {
    companion object {
        <!SYNTAX!>when<!> <!SYNTAX!>(<!><!SYNTAX!>1<!><!SYNTAX!>)<!> <!SYNTAX!><!>{
            0 <!SYNTAX!>-> true<!><!SYNTAX!>;<!>
            <!SYNTAX!>else<!> <!SYNTAX!>-> false<!><!SYNTAX!>;<!>
        }
    }
}

// Invalid when declaration
<!SYNTAX!>when<!> <!SYNTAX!>(<!><!SYNTAX!>1<!><!SYNTAX!>)<!> <!SYNTAX!><!>{
    0 <!SYNTAX!>-> true<!><!SYNTAX!>;<!>
    2 <!SYNTAX!>-> false<!><!SYNTAX!>;<!>
}

/* GENERATED_FIR_TAGS: andExpression, asExpression, classDeclaration, companionObject, comparisonExpression,
disjunctionExpression, enumDeclaration, enumEntry, equalityExpression, functionDeclaration, guardCondition,
integerLiteral, isExpression, localProperty, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration,
rangeExpression, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
