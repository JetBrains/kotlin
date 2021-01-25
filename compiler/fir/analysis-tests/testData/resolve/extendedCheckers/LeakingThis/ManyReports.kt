// !DUMP_CFG
class My(var x: String) {

    var y: String
        get() = if (x != "") x else <!MAY_BE_NOT_INITIALIZED!>z<!>
        set(arg) {
            if (arg != "") x = arg
        }

    val z: String

    var d: String = ""
        get
        set

    val z1: String

    init {

        // Dangerous: getter!
        if (<!LEAKING_THIS!>y<!> != "") z = <!LEAKING_THIS{PSI}!>this.<!LEAKING_THIS{LT}!>y<!><!> else z = <!LEAKING_THIS!>y<!>
        z = ""
    }
}
