class A {
    var x = 0
        set(value) {
            x++
            x+=1
            x = value
        }

    var y = 0
        get() {
            println("$y")
            y++
            y += 1
            return y
        }

    var z = 0
        get() = z

    var field = 0
        get() {
            return if (field != 0) field else -1
        }
        set(value) {
            if (value >= 0) field = value
        }
}