// RUN_PIPELINE_TILL: BACKEND
enum class MyEnum {
    FIRST,
    SECOND,
    LAST;

    fun bar() = 42
}

fun foo() {
    val values = MyEnum.values()

    for (value in values) {
        value.bar()
    }

    val first = MyEnum.valueOf("FIRST")
    val last = MyEnum.valueOf("LAST")
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, forLoop, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
