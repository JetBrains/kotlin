// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
fun launch(f : () -> Unit) {
    f()
}

fun box(): String {
    val list = ArrayList<Int>()
    val foo : () -> Unit = {
        list.add(2)  //first exception
    }
    foo()

    launch({
        list.add(3)
    })

    val bar = {
        val x = 1   //second exception
    }
    bar()

    return if (list.size == 2 && list.get(0) == 2 && list.get(1) == 3) "OK" else "fail"
}
