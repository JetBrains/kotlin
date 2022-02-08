class Immutable(val x: String?) {
    fun foo(): String {
        if (x != null) return x
        return ""
    }
}

class Mutable(var y: String?) {
    fun foo(): String {
        if (y != null) return <!SMARTCAST_IMPOSSIBLE!>y<!>
        return ""
    }
}