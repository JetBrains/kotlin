// FIR_DISABLE_LAZY_RESOLVE_CHECKS
fun test(map: java.util.AbstractMap<String, Int>) {
    map.remove("", null)
    map.remove(null)
}