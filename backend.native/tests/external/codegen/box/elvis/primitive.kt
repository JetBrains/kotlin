fun box(): String {
    if ((42 ?: 239) != 42) return "Fail Int"
    if ((42.toLong() ?: 239.toLong()) != 42.toLong()) return "Fail Long"
    
    return "OK"
}
