fun box(): String {
    val sb = StringBuilder()
    
    for (i in -1..1) {
        for (j in -1..1) {
            val a = i compareTo j
            val b = i.compareTo(j)
            if (a != b) {
                sb.append("$i compareTo $j:  $a != $b\n")
            }
        }
    }
    
    if (sb.length() == 0) return "OK"
    return "Fail:\n$sb"
}
