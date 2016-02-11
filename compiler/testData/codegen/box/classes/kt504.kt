// Enable for JS when it supports Java class library.
// TARGET_BACKEND: JVM
// BTW, this test does not seem relevant to real descsription of issue #KT-504
import java.util.HashMap
import java.io.*

operator fun <K, V> MutableMap<K, V>.set(key : K, value : V) = put(key, value)

fun box() : String {

    val commands : MutableMap<String, String> = HashMap()

    commands["c1"]  = "239"
    if(commands["c1"] != "239") return "fail"

    commands["c1"] += "932"
    return if(commands["c1"] == "239932") "OK" else "fail"
}
