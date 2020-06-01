private var cOrDCounter = 0

fun getCorD() = if (cOrDCounter++ % 2 == 0) getC() else getD()