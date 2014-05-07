fun box(): String {
    (::ByteArray)(10)
    (::IntArray)(10)
    (::ShortArray)(10)
    (::LongArray)(10)
    (::DoubleArray)(10)
    (::FloatArray)(10)
    (::BooleanArray)(10)

    return "OK"
}
