// TARGET_BACKEND: JVM
// FULL_JDK

class MapWithBadDefaults : HashMap<String, String>() {
    override fun getOrDefault(key: String, defaultValue: String): String {
        throw RuntimeException("Shouldn't be executed")
    }

    override fun remove(key: String, value: String): Boolean {
        throw RuntimeException("Shouldn't be executed")
    }
}


class Test(map: MutableMap<String, String>) : MutableMap<String, String> by map

fun box(): String {
    val test = Test(MapWithBadDefaults())
    test.put("O", "K")
    if (!test.containsKey("O")) return "fail 1: can't find value for key 'O'"
    if (!test.remove("O", "K")) return "fail 2: entry wasn't removed"

    return test.getOrDefault("absent", "OK")
}