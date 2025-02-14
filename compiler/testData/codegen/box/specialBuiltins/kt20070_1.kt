// WITH_STDLIB
// TARGET_BACKEND: JVM

class KtMap : AbstractMap<String, String>() {
    override val entries: HashSet<Map.Entry<String, String>>
        get() = HashSet<Map.Entry<String, String>>().apply {
            add(object : Map.Entry<String, String> {
                override val key: String
                    get() = "O"
                override val value: String
                    get() = "K"
            })
        }
}

fun box(): String {
    val entry = KtMap().entries.first()
    return entry.key + entry.value
}