// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    Option1, Option2, Option3;
    companion object {
        val enumProp: MyEnum = Option1
        val stringProp: String = ""
        fun getOption() = Option1
    }
}

val EnumOptionAlias = MyEnum.Option1

val OptionAlias = MyEnum.Option2

val tlEnumProp1: MyEnum = Option1
val tlEnumProp2: MyEnum = OptionAlias
val tlEnumProp3: MyEnum = enumProp
val topLevelProp4: MyEnum <!INITIALIZER_TYPE_MISMATCH!>=<!> stringProp

fun testReassignment() {
    var m: MyEnum = Option1
    m = Option1
    m = OptionAlias
    m <!ASSIGNMENT_TYPE_MISMATCH!>=<!> stringProp
}

fun testCorrectDefaultParam(arg1: MyEnum = Option1, arg2: MyEnum = enumProp) {}
fun testWrongDefaultParam(arg: MyEnum <!INITIALIZER_TYPE_MISMATCH!>=<!> stringProp) {}

class PropOwner(val prop: MyEnum = Option2) {
    val prop2: MyEnum = enumProp
    val prop3: MyEnum <!INITIALIZER_TYPE_MISMATCH!>=<!> stringProp
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration,
localProperty, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
