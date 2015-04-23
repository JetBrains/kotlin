class Bar {
    fun next(): Bar? {
        if (2 == 4)
            return this
        else
            return null
    }
}

fun foo(): Bar {
    var x: Bar? = Bar()
    var y: Bar?
    y = Bar()
    while (x != null) {
        // Here call is unsafe because of inner loop
        y<!UNSAFE_CALL!>.<!>next()
        while (y != null) {
            if (x == y)
                // x is not null because of outer while
                return <!DEBUG_INFO_SMARTCAST!>x<!>
            // y is not null because of inner while
            y = <!DEBUG_INFO_SMARTCAST!>y<!>.next()
        }
        // x is not null because of outer while
        x = <!DEBUG_INFO_SMARTCAST!>x<!>.next()
    }
    return Bar()
}