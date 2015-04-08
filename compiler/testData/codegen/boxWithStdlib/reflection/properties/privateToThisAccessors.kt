import kotlin.reflect.jvm.*

class K<in T : String> {
    private var t: T
        get() = "OK" as T
        set(value) {}

    fun run(): String {
        val p = ::t
        p.accessible = true
        p.set(this, "" as T)
        return p.get(this)
    }
}

fun box() = K<String>().run()
