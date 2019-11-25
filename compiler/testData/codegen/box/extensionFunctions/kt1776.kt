// IGNORE_BACKEND_FIR: JVM_IR
interface Expr {
    public fun ttFun() : Int = 12
}

class Num(val value : Int) : Expr

fun Expr.sometest() : Int {
    if (this is Num) {
        value
        return value
    }
    return 0;
}


fun box() : String {
    if (Num(11).sometest() != 11) return "fail ${Num(11).sometest()}"

    return "OK"
}