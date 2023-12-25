fun topLevelMethod() {
    var z = 1
    if(true) { z = 2 } else { z = 3 }
    if(true)   z = 4   else   z = 5

    if(true) { z += 6 } else { z += 7 }
    if(true)   z += 8   else   z += 9

    when(z) {
        1 -> { z = 10 }
        else -> { z = 11 }
    }

    when(z) {
        1 -> z = 12
        else -> z = 13
    }

    when(z) {
        1 -> { z += 14 }
        else -> { z += 15 }
    }

    when(z) {
        1 -> z += 16
        else -> z += 17
    }
}