class My(var x: String) {

    var y: String
        get() = if (x != "") x else z
        set(arg) {
            if (arg != "") x = arg
        }

    val z: String

    var d: String = ""
        get
        set

    val z1: String

    init {
        <!DEBUG_INFO_LEAKING_THIS!>d<!> = "d"
        if (<!DEBUG_INFO_LEAKING_THIS!>d<!> != "") z1 = this.<!DEBUG_INFO_LEAKING_THIS!>d<!> else z1 = <!DEBUG_INFO_LEAKING_THIS!>d<!>

        // Dangerous: setter!
        <!DEBUG_INFO_LEAKING_THIS!>y<!> = "x"
        // Dangerous: getter!
        if (<!DEBUG_INFO_LEAKING_THIS!>y<!> != "") z = this.<!DEBUG_INFO_LEAKING_THIS!>y<!> else z = <!DEBUG_INFO_LEAKING_THIS!>y<!>
    }
}
