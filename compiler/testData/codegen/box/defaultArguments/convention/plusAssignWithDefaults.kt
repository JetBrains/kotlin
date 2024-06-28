// LANGUAGE: +ProperArrayConventionSetterWithDefaultCalls
var inc: String = ""

class X {
    var result: String = "fail"

    operator fun get(name: String, type: Int = 100) = name + type

    operator fun set(name: String, defaultParam: String = "_default_", value: String) {
        result = name + defaultParam + value;
    }
}

class Y {
    var result: String = "fail"

    operator fun get(name: String, type: String = "_default_", type2: Int = 2) = name + type + type2

    operator fun set(name: String, defaultParam: String = "_default_in_setter_", value: String) {
        result = name + defaultParam + value;
    }
}

fun box(): String {
    var x = X()
    x["index"] += "OK"
    if (x.result != "index_default_index100OK") return "fail 1: ${x.result}"

    var y = Y()
    y["index"] += "OK"
    if (y.result != "index_default_in_setter_index_default_2OK") return "fail 2: ${y.result}"

    return "OK"
}