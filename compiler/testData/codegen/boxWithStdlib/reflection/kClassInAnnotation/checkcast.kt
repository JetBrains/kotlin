import kotlin.reflect.KClass

fun box(): String {
    try {
        String::class.java as KClass<String>
    } catch (e: Exception) {
        return "OK"
    }
    return "fail"
}
