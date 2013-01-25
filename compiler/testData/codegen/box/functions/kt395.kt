fun Any.with(operation :  Any.() -> Any) = operation().toString()

val f = { (a : Int) :Unit -> }

fun box () : String {
    return if(20.with {
        this
    } == "20")
        "OK"
    else
        "fail"
}
