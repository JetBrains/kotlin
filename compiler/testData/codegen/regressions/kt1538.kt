import java.util.HashMap

fun parseCatalogs(hashMap: Any?) {
    val r = toHasMap(hashMap)
    if (!r._1) {
        return
    }
    val nodes = r._2
}

fun toHasMap(value: Any?): #(Boolean, HashMap<String, Any?>?) {
    if(value is HashMap<*, *>) {
        return #(true, value as HashMap<String, Any?>)
    }
    return #(false, null as HashMap<String, Any?>?)
}

fun box() : String {
    parseCatalogs(null)
    return "OK"
}