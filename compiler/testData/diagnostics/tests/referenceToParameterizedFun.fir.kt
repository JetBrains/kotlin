// ISSUE: KT-59233

fun <T> consume(arg: T) {}

fun box(): String {
    val foo = ::consume
    return "OK"
}
