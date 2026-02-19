// TARGET_BACKEND: WASM

object IntParam : Param<Int> {
    override val valueExample get() = 1
    override val isNullable get() = true
}

interface Param<T: Any> {
    val valueExample: T
    val isNullable: Boolean
}

fun <T> queryParam(param: Param<T & Any>): T {
    if (param.isNullable) {
        return null as T
    } else {
        return param.valueExample
    }
}

fun box(): String {
    return try {
        queryParam(IntParam).toString()
    } catch (e: ClassCastException) {
        "OK"
    }
}