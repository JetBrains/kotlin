class Immutable(val x: String?) {
    fun foo(): String {
        if (x != null) return <!DEBUG_INFO_SMARTCAST!>x<!>
        return ""
    }
}

class Mutable(var y: String?) {
    fun foo(): String {
        if (y != null) return <!SMARTCAST_IMPOSSIBLE!>y<!>
        return ""
    }
}