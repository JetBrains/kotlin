// FULL_JDK

fun test(map: MutableMap<Int, MutableMap<Int, Int>>) {
    map.getOrPut(1, ::mutableMapOf)
}
