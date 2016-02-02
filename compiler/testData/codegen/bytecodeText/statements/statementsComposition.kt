fun print(v: Any?) {}

fun foo() {
    val array = intArrayOf(1,2,3)

    var count = 0
    for (element in array) if (element > 0) count++
    while (count > 0) if (count > 0) count++

    if (count == 1)
        if (count != 1)
            count++
        else
            print("1")
        else
            print("2")

    when (count) {
        1 -> if (count == 1) count++ else print("123")
        else -> if (count == 1) count++ else print("123")
    }

    for (element in array)
        when (element) {
            1 -> if (count == 1) count++ else print("123")
            else -> if (count == 1) count++ else print("123")
        }

    while (count > 0)
        when (count) {
            1 -> if (count == 1) count++ else print("123")
            else -> if (count == 1) count++ else print("123")
        }
}

// 0 valueOf
// 0 GETSTATIC
