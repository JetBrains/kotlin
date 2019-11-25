// IGNORE_BACKEND_FIR: JVM_IR
//WITH_RUNTIME

fun box(): String {
    var methodVar = "OK"

    fun localMethod() : String
    {
        return lazy { methodVar }::value.get()
    }

    return localMethod()
}