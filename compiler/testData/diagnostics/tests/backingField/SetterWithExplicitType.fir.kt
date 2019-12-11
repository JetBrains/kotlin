// KT-7042 Providing return type for property setter is not reported as error

var x: Int = 1

// No backing field!
var y: Int
    get() = x
    set(value): Any {
        x = value
    }

var z: Int
    get() = x
    set(value): Unit {
        x = value
    }

var u: String = ""
    set(value): Unit {
        field = value
    }

var v: String = ""
    set(value): String {
        field = value
    }