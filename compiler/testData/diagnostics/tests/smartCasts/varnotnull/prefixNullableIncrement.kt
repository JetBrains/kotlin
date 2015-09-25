operator fun Int?.inc(): Int? { return this }

public fun box(arg: Int?) : Int? {
    var i = arg
    var j = ++i
    j<!UNSAFE_CALL!>.<!>toInt()
    return ++j
}
