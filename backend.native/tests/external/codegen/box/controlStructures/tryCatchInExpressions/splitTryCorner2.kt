fun shouldReturn11() : Int {
    var x = 0
    while (true) {
        try {
            if(x < 10)
                x++
            else
                break
        }
        finally {
            x++
        }
    }
    return x
}

fun box(): String {
    val test = shouldReturn11()
    if (test != 11) return "Failed, test=$test"

    return "OK"
}
