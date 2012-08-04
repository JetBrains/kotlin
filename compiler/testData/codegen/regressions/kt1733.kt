fun box() : String {
        val map = java.util.TreeMap<String, String>()
        map["a"] = "1"
        map["b"] = "2"

        var list = arrayList<String>()
        for (e in map) {
            list += e.getKey()
        }

        return if(list.size==2) "OK" else "fail"
}
