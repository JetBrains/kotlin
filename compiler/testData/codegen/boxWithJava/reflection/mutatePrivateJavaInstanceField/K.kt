import kotlin.reflect.*
import kotlin.reflect.jvm.*

fun box(): String {
    val a = J()
    val p = J::class.members.single { it.name == "result" } as KMutableProperty1<J, String>
    p.isAccessible = true
    p[a] = "OK"
    return p[a]
}
