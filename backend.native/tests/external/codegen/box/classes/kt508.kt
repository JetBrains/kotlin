// TODO: Enable for JS when it supports Java class library.
// IGNORE_BACKEND: JS
// fails on JS with TypeError: imported$plus is not a function, it is undefined.

operator fun <K, V> MutableMap<K, V>.set(key : K, value : V) = put(key, value)

fun box() : String {

    val commands : MutableMap<String, String> = HashMap()

    commands["c1"]  = "239"
    if(commands["c1"] != "239") return "fail"

    commands["c1"] += "932"
    return if(commands["c1"] == "239932") "OK" else "fail"
}
