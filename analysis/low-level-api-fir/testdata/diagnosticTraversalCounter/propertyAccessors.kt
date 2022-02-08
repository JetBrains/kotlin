var x: Int = 10
    get() = field
    set(value) {
        println(1)
        field = value
    }

class X {
    var y: Int = 10
        get() = field
        set(value) {
            println(2)
            field = value
        }
}