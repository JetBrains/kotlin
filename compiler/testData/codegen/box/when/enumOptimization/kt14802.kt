// CHECK_CASES_COUNT: function=crash count=2 TARGET_BACKENDS=JS
// CHECK_IF_COUNT: function=crash count=1 TARGET_BACKENDS=JS

class EncapsulatedEnum<T : Enum<T>>(val value: T)

enum class MyEnum(val value: String) {
    VALUE_A("OK"),
    VALUE_B("fail"),
}

private fun crash(encapsulated: EncapsulatedEnum<*>) {
    val myEnum = encapsulated.value
    if (myEnum !is MyEnum) {
        return
    }

    when (myEnum) {
        MyEnum.VALUE_A -> res = myEnum.value
        MyEnum.VALUE_B -> res = myEnum.value
    }
}

var res = "fail"

fun box(): String {
    crash(EncapsulatedEnum(MyEnum.VALUE_A))
    return res
}
