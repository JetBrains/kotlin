// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315

enum class MyEnum {
    EnumValue1;
}

fun <T: MyEnum> boundHolder(arg: T) = arg

fun testFunBound() {
    boundHolder(EnumValue1)
}

fun testFunBound2() {
    boundHolder<MyEnum>(EnumValue1)
}

<!NOTHING_TO_INLINE!>inline<!> fun <T: MyEnum> testInlineFunBound(value: T) {
    value == EnumValue1
}

fun <T: MyEnum> testFunBound(value: T) {
    value == EnumValue1
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, inline, typeConstraint,
typeParameter */
