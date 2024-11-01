// LANGUAGE: -ProperUninitializedEnumEntryAccessAnalysis

fun test() {
    enum class MyEnum {
        A;

        val someProperty = 10
    }

    MyEnum.A.someProperty
}
