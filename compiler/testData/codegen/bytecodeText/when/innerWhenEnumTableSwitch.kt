// See KT-62903
// IGNORE_BACKEND_K1: JVM_IR

enum class Level {
    O,
    K
}

private fun Any?.doTheThing(): String {
    when (this) {
        is String -> return this
        is Level -> {
            when (this) {
                Level.O -> return Level.O.name
                Level.K -> return Level.K.name
            }
        }

        else -> return "fail"
    }
}

// 1 TABLESWITCH