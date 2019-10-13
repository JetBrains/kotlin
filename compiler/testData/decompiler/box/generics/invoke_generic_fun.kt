fun box(): String {
    val listStr = ArrayList<String>()
    listStr.add("one")
    listStr.add("two")
    listStr.add("three")
    if (listStr.size == 3) {
        return "OK"
    } else {
        return "FAIL"
    }
}

