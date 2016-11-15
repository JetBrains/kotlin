private var globalValue = 1
var global:Int
    get() = globalValue
    set(value:Int) {globalValue = value}

fun globalTest(i:Int):Int {
    global += i
    return global
}