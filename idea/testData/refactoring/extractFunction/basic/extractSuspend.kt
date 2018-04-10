// PARAM_DESCRIPTOR: public suspend inline fun <reified T> ExtendMe.receive(): T defined in root package in file extractSuspend.kt
// PARAM_TYPES: ExtendMe
suspend inline fun <reified T> ExtendMe.receive(): T = <selection>receive("")</selection> as T

class ExtendMe {
    suspend fun receive(s: String): Any {
        return Any()
    }
}