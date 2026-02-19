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

fun <T>receive(e: T) {}
val EnumOptionAlias = MyEnum.Option1

fun testElvis() {
    var i10: MyEnum = Option1 <!USELESS_ELVIS!>?: Option2<!>
    var i11: MyEnum? = Option1 <!USELESS_ELVIS!>?: Option2<!>
    var i20: MyEnum = enumProp <!USELESS_ELVIS!>?: Option2<!>
    var i30: MyEnum <!INITIALIZER_TYPE_MISMATCH!>=<!> enumProp <!USELESS_ELVIS!>?: stringProp<!>
    var i40: MyEnum <!INITIALIZER_TYPE_MISMATCH!>=<!> stringProp <!USELESS_ELVIS!>?: enumProp<!>
    var i50: MyEnum = <!UNRESOLVED_REFERENCE!>getOption<!>() ?: enumProp

    receive<MyEnum>(Option1 <!USELESS_ELVIS!>?: Option2<!>)
    receive<MyEnum>(enumProp <!USELESS_ELVIS!>?: <!ARGUMENT_TYPE_MISMATCH!>stringProp<!><!>)
    receive<MyEnum>(enumProp <!USELESS_ELVIS!>?: <!UNRESOLVED_REFERENCE!>getOption<!>()<!>)
}

/* GENERATED_FIR_TAGS: companionObject, elvisExpression, enumDeclaration, enumEntry, functionDeclaration,
intersectionType, localProperty, nullableType, objectDeclaration, propertyDeclaration, stringLiteral, typeParameter */
