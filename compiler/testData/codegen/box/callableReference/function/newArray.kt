// IGNORE_BACKEND: JVM_IR
private fun <T> upcast(value: T): T = value

fun box(): String {
    upcast<(Int)->ByteArray>(::ByteArray)(10)
    upcast<(Int)->IntArray>(::IntArray)(10)
    upcast<(Int)->ShortArray>(::ShortArray)(10)
    upcast<(Int)->LongArray>(::LongArray)(10)
    upcast<(Int)->DoubleArray>(::DoubleArray)(10)
    upcast<(Int)->FloatArray>(::FloatArray)(10)
    upcast<(Int)->BooleanArray>(::BooleanArray)(10)

    return "OK"
}
