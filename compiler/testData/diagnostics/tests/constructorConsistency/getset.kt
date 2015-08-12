class My(var x: String) {

    var y: String
        get() = if (x != "") x else z
        set(arg) {
            if (arg != "") x = arg
        }

    val z: String

    init {
        // Dangerous: setter!
        <!DEBUG_INFO_LEAKING_THIS!>y<!> = "x"
        // Dangerous: getter!
        if (<!DEBUG_INFO_LEAKING_THIS!>y<!> != "") z = this.<!DEBUG_INFO_LEAKING_THIS!>y<!> else z = <!DEBUG_INFO_LEAKING_THIS!>y<!>
    }
}
