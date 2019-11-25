// IGNORE_BACKEND_FIR: JVM_IR
abstract class WaitFor {
    init {
        condition()
    }

    abstract fun condition() : Boolean;
}

fun box(): String {
    val local = ""
    var result = "fail"
    val s = object: WaitFor() {

        override fun condition(): Boolean {
            result = "OK"
            return result.length== 2
        }
    }

    return result;
}
