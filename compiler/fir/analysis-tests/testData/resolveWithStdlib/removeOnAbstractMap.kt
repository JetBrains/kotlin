// FULL_JDK

fun test(map: java.util.AbstractMap<String, Int>) {
    map.remove("", null)
    map.remove(null)
}
