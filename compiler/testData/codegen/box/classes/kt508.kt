// KJS_WITH_FULL_RUNTIME

operator fun <K, V> MutableMap<K, V>.set(key : K, value : V) = put(key, value)

fun box() : String {

    val commands : MutableMap<String, String> = HashMap()

    commands["c1"]  = "239"
    if(commands["c1"] != "239") return "fail"

    commands["c1"] += "932"
    return if(commands["c1"] == "239932") "OK" else "fail"
}
