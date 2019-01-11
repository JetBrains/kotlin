fun box(): String {
    var result = 0
    if (1 == 1) {
        val x: Int? = 1
        result += x!!
    }
    return "OK"
}

// 0 java/lang/Integer.valueOf
