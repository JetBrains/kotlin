// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var x = 0
    do x++ while (x < 5)
    if (x != 5) return "Fail 1 $x"
    
    var y = 0
    do { y++ } while (y < 5)
    if (y != 5) return "Fail 2 $y"
    
    var z = ""
    do { z += z.length } while (z.length < 5)
    if (z != "01234") return "Fail 3 $z"
    
    return "OK"
}
