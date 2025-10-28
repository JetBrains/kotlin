// RUN_PIPELINE_TILL: FRONTEND

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
    val result = when (testParam) {
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
        else -> "No use"
    }
}

fun expressionWhenWithIncompatibleType() {
    val flag = 1
    val result = when (flag) {
        true -> "true"
        false -> "false"
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
    val numericValue = when (Fruite.ORANGE) {
        Fruite.APPLE -> "apple"
    }
}

fun expressionWhenWithEnumWithInvalidInputType() {
    val numericValue = when (1) {
        Fruite.APPLE -> "apple"
        Fruite.ORANGE -> "orange"
    }
}

fun expressionWhenWithEnumWithInvalidInputTypeAndElse() {
    val numericValue = when (1) {
        Fruite.APPLE -> "apple"
        Fruite.ORANGE -> "orange"
        else -> "Else"
    }
}

fun expressionWhenWithContextSensitiveResolution(fruiete: Fruite):String {
    val numericValue = when (fruiete) {
        APPLE -> "apple"
        ORANGE -> "orange"
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
    return when(Animal) {
        is Dog -> "Dog"
        is Cat -> "Cat"
    }
}

fun expressionWhenWithSealedClassAndPartOptionsIncluded(animal: Animal): String {
    return when(animal) {
        is Dog -> "Dog"
    }
}

fun expressionWhenWithSealedClassWithInvalidInputType(animal: String): String {
    return when(animal) {
        is Dog -> "Dog"
        is Cat -> "Cat"
    }
}

fun expressionWhenWithSealedClassWithInvalidInputTypeWithElse(animal: String): String {
    return when(animal) {
        is Dog -> "Dog"
        is Cat -> "Cat"
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
    return when(anotherAnimal) {
        is AnotherDog -> "AnotherDog"
        is AnotherCat -> "AnotherCat"
    }
}

fun expressionWhenWithOpenClassWithElseAndInvalidInputType(anotherAnimal: String): String {
    return when(anotherAnimal) {
        is AnotherDog -> "AnotherDog"
        is AnotherCat -> "AnotherCat"
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
    val result = when {
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
        "Car", "Wathces", "House" -> print ("Not Money")
        "Money", "Dinnero" -> "Money!"
        else -> "Happy"
    }
}

fun expressionWhenWithMulltipleCheckWithoutElse() {
    val searchFor = "Money"
    val result = when (searchFor) {
        "Car", "Wathces", "House" -> print ("Not Money")
        "Money", "Dinnero" -> "Money!"
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

    val result = when (testValue) {
        1.toString() -> "One"
    }
}

fun expressionWhenWithInvalidCast() {
    val testValue = "1"

    val result = when (testValue) {
        1 as String -> "One"
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
        is String -> "This is STRIIIIIIING!"
        else -> "Not String"
    }
}

fun expressionWhenWithIncorrectTypeCheck() {
    val testValue: String = "testValue"

    val result = when(testValue) {
        is -> "This is INTEGER!"
        else -> "String?"
    }
}

fun expressionWhenWithTypeCheckWithoutElse() {
    val testValue: String = "testValue"

    val result = when(testValue) {
        is Integer -> "This is STRIIIIIIING!"
    }
}


fun expressionWhenWithTypeCheckSmartCast(testValue: Any) {
    val result = when(testValue) {
        is String -> testValue.startsWith("test")
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
        is Integer -> print("First value is Integer")
        is Integer if secondTestValue -> print("Condition Failed")
        is String if secondTestValue -> print("Condition Passed")
        else -> "What are we?"
    }
}

fun guardExpressionWithDoubleCheck() {
    val firstTestValue = "1"
    val secondTestValue = true
    val thirdTestValue = false

    when(firstTestValue) {
        is Integer -> print("First value is Integer")
        is Integer if secondTestValue -> print("Condition Failed")
        is String if (secondTestValue && !thirdTestValue) -> print("Condition Passed")
        else -> "What are we?"
    }
}

fun guardExpressionWithTwoOptionsCheck() {
    val firstTestValue = "1"
    val secondTestValue = false
    val thirdTestValue = true

    when(firstTestValue) {
        is Integer -> print("First value is Integer")
        is Integer if secondTestValue -> print("Condition Failed")
        is String if secondTestValue || thirdTestValue -> print("Condition Passed")
        else -> "What are we?"
    }
}

fun guardExpressionWithTwoOptionsCheckAndIfElse() {
    val firstTestValue = "1"
    val secondTestValue = false
    val thirdTestValue = false
    val fallbackValue = false

    when(firstTestValue) {
        is Integer -> print("First value is Integer")
        is Integer if secondTestValue -> print("Condition Failed")
        is String if secondTestValue || thirdTestValue -> print("Condition Failed")
        else if (fallbackValue) -> print("Condition Passed")
        else -> "What are we?"
    }
}

fun statementWhenWithNullAsValue() {
    val nullValue = null
    when(nullValue) {
        is String -> print("First value is String")
        else -> print("Else")
    }
}

fun expressionWhenWithBooleanAndCheckForNull(fruite: Fruite): String {
    val nullValue = null
    return when(nullValue) {
        is null -> "Fails"
        else -> "Else"
    }
}

fun statementWhenWithNothingAsValue() {
    val nullValue = Nothing
    when(nullValue) {
        is String -> print("First value is String")
        else -> print("Else")
    }
}

fun statementWhenWithVoidAsValue() {
    val nullValue = Void
    when(nullValue) {
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
    when (1) {
        0 -> true;
        else -> false;
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
        when (1) {
            0 -> true;
            else -> false;
        }
    }
}

// Invalid when declaration
when (1) {
    0 -> true;
    2 -> false;
}

/* GENERATED_FIR_TAGS: andExpression, asExpression, classDeclaration, companionObject, comparisonExpression,
disjunctionExpression, enumDeclaration, enumEntry, equalityExpression, functionDeclaration, guardCondition,
integerLiteral, isExpression, localProperty, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration,
rangeExpression, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
