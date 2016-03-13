data class A(val x: Array<Int>?, val y: IntArray?)

fun box(): String {
    var ts = A(Array<Int>(2, {it}), IntArray(3)).toString()
    if(ts != "A(x=[0, 1], y=[0, 0, 0])") return ts

    ts = A(null, IntArray(3)).toString()
    if(ts != "A(x=null, y=[0, 0, 0])") return ts

    ts = A(null, null).toString()
    if(ts != "A(x=null, y=null)") return ts

    return "OK"
}
