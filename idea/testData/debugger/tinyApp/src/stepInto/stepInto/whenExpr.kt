package whenExpr

fun main(args: Array<String>) {
    //Breakpoint!
    val a: Int? = 0         // 5
    when (a) {              // 6
        1 -> {              // 7
            1 + 1
        }
        2 -> {              // 10
            2 + 1
        }
        else -> {
            0 + 0           // 14
        }
    }
    val b = 1               // 17

    val a1: Number? = 0     // 19
    when (a1) {             // 20
        is Float -> {       // 21
            1 + 1
        }
        is Double -> {      // 24
            2 + 1
        }
        else -> {
            0 + 0           // 28
        }
    }
    val b1 = 1              // 31

    val a2: Int? = 2        // 33
    when (a2) {             // 34
        1 -> {              // 35
            1 + 1
        }
        2 -> {              // 38
            2 + 1           // 39
        }
        else -> {
            0 + 0
        }
    }
    val b2 = 1              // 45

    val a3: Number? = 0f    // 47
    when (a3) {             // 48
        is Float -> {       // 49
            1 + 1           // 50
        }
        is Double -> {
            2 + 1
        }
        else -> {
            0 + 0
        }
    }
    val b3 = 1              // 59
}

// STEP_INTO: 22