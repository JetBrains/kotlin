val x = when (true) {
    true -> {
        val localProp = 10
        fun localFun() {}
        class LocalClass {}
        typealias LocalTypealias = String
        1
    }
    else -> 2
}
