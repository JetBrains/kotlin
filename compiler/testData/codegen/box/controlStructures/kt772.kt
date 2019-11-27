// IGNORE_BACKEND_FIR: JVM_IR
package demo2

fun print(o : Any?) {}

fun test(i : Int) {
    var monthString : String? = "<empty>"
        when (i) {
        1 -> {
            print(1)
            print(2)
            print(3)
            print(4)
            print(5)
        }
       else -> {
            monthString = "Invalid month"
        }
    }
    print(monthString)
}

fun box() : String {
    for (i in 1..12) test(i)
    return "OK"
}
