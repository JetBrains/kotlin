// IGNORE_REVERSED_RESOLVE
class My(val v: Int) {
    // Ok: setter is just private
    var x: Int
        private set

    <!MUST_BE_INITIALIZED!>var y: Int<!>
        set(arg) { field = arg }

    <!MUST_BE_INITIALIZED!>var z: Int<!>
        set(arg) { field = arg }

    // Ok: initializer available
    var w: Int = v
        set(arg) { field = arg }

    // Ok: no backing field
    var u: Int
        get() = w
        set(arg) { w = 2 * arg }

    constructor(): this(0) {
        z = v        
    }

    init {
        x = 1
        y = 2
        u = 3
    }
}
