// IGNORE_BACKEND_FIR: JVM_IR
fun Any.with(operation :  Any.() -> Any) = operation().toString()

val f = { a : Int -> }

fun box () : String {
    return if(20.with {
        this
    } == "20")
        "OK"
    else
        "fail"
}
