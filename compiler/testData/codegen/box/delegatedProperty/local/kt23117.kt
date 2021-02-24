// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.properties.Delegates.notNull

fun box(): String {
    var bunny by notNull<String>()

    val obj = object {
        val getBunny = { bunny }
    }

    bunny = "OK"
    return obj.getBunny()
}
