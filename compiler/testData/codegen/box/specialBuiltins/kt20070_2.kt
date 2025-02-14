// WITH_STDLIB
// TARGET_BACKEND: JVM
abstract class MyMap : AbstractMap<String, String>() {
    override val keys: Set<String>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<String>
        get() = TODO("Not yet implemented")

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: String): String? {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsKey(key: String): Boolean {
        TODO("Not yet implemented")
    }

}

abstract class MyMap1 : MyMap(), Map<String, String>
abstract class MyMap2 : MyMap1()
class KtMap : MyMap2() {
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