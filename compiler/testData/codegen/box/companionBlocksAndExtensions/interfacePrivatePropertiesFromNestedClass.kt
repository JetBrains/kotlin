// LANGUAGE: +CompanionBlocks

interface I {
    companion {
        private val privateVal = "OK"
    }

    class NestedClass {
        fun read() = privateVal
    }
}

interface I2 {
    companion {
        private val privateVal = "OK"
    }

    companion object {
        fun read() = privateVal
    }
}

fun box(): String {
    if (I.NestedClass().read() != "OK") return "fail 1"
    if (I2.read() != "OK") return "fail 2"
    return "OK"
}
