enum class MyEnum {
    A, B, C
}

fun test(e: MyEnum?) {
    <caret>when (e) {
    }
}