private var globalValue = 1
var global:Int
    get() = globalValue
    set(value:Int) {globalValue = value}

fun globalTest(i:Int):Int {
    global += i
    return global
}


fun main(args:Array<String>) {
    if (global != 1)          throw Error()
    if (globalTest(41) != 42) throw Error()
    if (global != 42)         throw Error()
}