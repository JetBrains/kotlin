abstract class My(val v: Int) {
    // Ok: variable is just abstract
    abstract var x: Int

    <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!>open var y: Int<!>

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>open var z: Int<!>

    // Ok: initializer available
    open var w: Int = v
        set(arg) { field = arg }    

    // Ok: no backing field, no initializer possible
    open var u: Int
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
