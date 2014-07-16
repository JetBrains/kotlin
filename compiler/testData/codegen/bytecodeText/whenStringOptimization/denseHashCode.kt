
fun foo() : Int {
    val x : String = "dsa"
    when (x) {
        "a" -> return 1
        "b" -> return 1
        "c" -> return 1
        "d" -> return 1
        "e" -> return 1
        "f" -> return 1
        else -> return -1
    }
}

// 1 TABLESWITCH
