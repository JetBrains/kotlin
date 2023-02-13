enum class MyEnum {
    First,
    Second
}

fun getValue() = MyEnum.First

var result = "Failed"

fun getLambda(): (Int) -> Unit =
    when (val value = getValue()) {
        MyEnum.Second -> { _ -> }
        MyEnum.First -> { _ ->
            if (value == MyEnum.First) {
                result = "OK"
            }
        }
    }

fun box(): String {
    getLambda().invoke(2)
    return result
}
