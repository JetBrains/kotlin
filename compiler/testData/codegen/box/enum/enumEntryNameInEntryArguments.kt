// ISSUE: KT-68354

enum class TestEnum(val argument: String) {
    OWN_NAME(OWN_NAME.name),
    QUALIFIED_NAME(TestEnum.QUALIFIED_NAME.name),
    NEXT_NAME(NEXT.name),
    NEXT("next"),
}

fun box(): String {
    if (TestEnum.OWN_NAME.argument != "OWN_NAME") return "Fail OWN_NAME: ${TestEnum.OWN_NAME.argument}"
    if (TestEnum.QUALIFIED_NAME.argument != "QUALIFIED_NAME") return "Fail QUALIFIED_NAME: ${TestEnum.QUALIFIED_NAME.argument}"
    if (TestEnum.NEXT_NAME.argument != "NEXT") return "Fail NEXT_NAME: ${TestEnum.NEXT_NAME.argument}"
    return "OK"
}
