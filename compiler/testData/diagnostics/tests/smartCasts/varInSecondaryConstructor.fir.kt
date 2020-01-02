class My(val z: Int) {
    var x: Int = 0
    constructor(arg: Int?): this(arg ?: 42) {
        var y: Int?
        y = arg
        if (y != null) {
            x = y
        }
    }
}