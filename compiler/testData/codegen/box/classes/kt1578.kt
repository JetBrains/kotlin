// IGNORE_BACKEND: JVM_IR
fun box() : String {
    var i = 0
        {
            i++
        }()
    i++  //the problem is here
//    i = i + 1  //this way it works
    return if (i == 2) "OK" else "fail"
}
