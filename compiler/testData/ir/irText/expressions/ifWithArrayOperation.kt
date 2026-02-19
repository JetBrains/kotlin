fun topLevelMethod() {
    val array = intArrayOf(1)

    if(true) { array[0] = 2 } else { array[0] = 3 }
    if(true)   array[0] = 4   else   array[0] = 5

    if(true) { array[0] += 6 } else { array[0] += 7 }
    if(true)   array[0] += 8   else   array[0] += 9

    when(array[0]) {
        1 -> { array[0] = 10 }
        else -> { array[0] = 11 }
    }

    when(array[0]) {
        1 -> array[0] = 12
        else -> array[0] = 13
    }

    when(array[0]) {
        1 -> { array[0] += 14 }
        else -> { array[0] += 15 }
    }

    when(array[0]) {
        1 -> array[0] += 16
        else -> array[0] += 17
    }
}