// IGNORE_BACKEND_FIR: JVM_IR
var holder = ""

var mainShape: Shape? = null

fun getShape(): Shape? {
    holder += "getShape1()"
    mainShape = Shape("fail")
    return mainShape
}

fun getOK(): String {
    holder += "->OK"
    return "OK"
}


class Shape(var result: String) {

    var innerShape: Shape? = null

    fun getShape2(): Shape? {
        holder += "->getShape2()"
        innerShape = Shape(result)
        return innerShape
    }
}

fun box(): String {
    getShape()?.getShape2()?.result = getOK();

    if (holder != "getShape1()->getShape2()->OK") return "fail $holder"

    return mainShape!!.innerShape!!.result
}