import kotlin.reflect.KClass

fun box(): String {
    try {
        javaClass<String>() as KClass<String>
    } catch (e: Exception) {
        return "OK"
    }
    return "fail"
}
