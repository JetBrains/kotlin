// ISSUE: KT-75316
// WITH_STDLIB
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType, +ContextParameters

enum class MyEnum {
    Option1, Option2, Option3;
    companion object {
        val enumProp: MyEnum = Option1
        val stringProp: String = ""
        fun getOption() = Option1
    }
}

val EnumOptionAlias = MyEnum.Option1

val enumLambda: (arg: Boolean) -> MyEnum = { arg -> if (arg) enumProp else Option1 }

val implicitReturnType: MyEnum = { arg: Boolean -> if (arg) enumProp else Option1 }(true)

val enumLambda2: (arg: Boolean) -> MyEnum = { arg ->
    enumProp
    if (arg) enumProp else Option1
}

val enumLambda3: (arg: Boolean) -> MyEnum = { arg ->
    try {
        enumProp
        if (arg) enumProp else Option1
    } catch (e: Exception) {
        Option1
    }
}

val enumAnonFuncProp: (arg: Boolean) -> MyEnum = fun(arg: Boolean): MyEnum {
    return if (arg) enumProp else Option1
}

val withContextProp : context(MyEnum)() -> Unit = {
    contextOf<MyEnum>() == Option1
}
