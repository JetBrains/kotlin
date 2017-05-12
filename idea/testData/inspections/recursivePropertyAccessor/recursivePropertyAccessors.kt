class A {
    var x = 0
        set(value) {
            x++
            x += 1
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

    var w
        set(value) {
            field = w + value
        }

    var field = 0
        get() {
            this.field
            return if (field != 0) field else -1
        }
        set(value) {
            this.field = value
            if (value >= 0) field = value
        }

    companion object {
        var g = 0
            set(value) {
                A.g = 99
            }
    }
}