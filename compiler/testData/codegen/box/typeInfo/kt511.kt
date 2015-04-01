package one_extends_base

open class Base<T>(name : T?) {
    var myName : T?
    init {
        $myName = name
    }
}
open class One<T, K>(name : T?, second : K?) : Base<T?>(name) {
    var mySecond : K?
    init {
        $mySecond = second
    }
}

fun box() = if(One<String, Int>("ola", 0).myName == "ola") "OK" else "fail"
