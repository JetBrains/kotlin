fun topLevelMethod() {
    var z = 1

    if(true) { for (i in 0..2) z += i } else { for (i in 0..3) z += i }
    if(true) for (i in 0..4) z += i else for (i in 0..5) z += i

    if(true) { while(false) z += 6 } else { while(false) z += 7 }
    if(true) while(false) z += 8 else while(false) z += 9

    if(true) { do { z += 10 } while(false) } else { do { z += 11 } while(false) }
    if(true) do { z += 12 } while(false) else do { z += 13 } while(false)

    when(z) {
        1 -> { for (i in 0..14) z += i }
        else -> { for (i in 0..15) z += i }
    }

    when(z) {
        1 -> for (i in 0..16) z += i
        else -> for (i in 0..17) z += i
    }

    when(z) {
        1 -> { while(false) z += 18 }
        else -> { while(false) z += 19 }
    }

    when(z) {
        1 -> while(false) z += 20
        else -> while(false) z += 21
    }

    when(z) {
        1 -> { do { z += 22 } while(false) }
        else -> { do { z += 23 } while(false) }
    }

    when(z) {
        1 -> do { z += 24 } while(false)
        else -> do { z += 25 } while(false)
    }
}