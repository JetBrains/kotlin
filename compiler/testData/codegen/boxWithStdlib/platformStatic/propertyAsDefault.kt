import kotlin.platform.platformStatic

object X {
    @platformStatic val x = "OK"

    fun fn(value : String = x): String = value
}

fun box(): String {
    return X.fn()
}