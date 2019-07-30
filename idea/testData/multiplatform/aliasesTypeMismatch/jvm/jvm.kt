actual typealias MyCancelException = platform.lib.MyCancellationException

actual open class OtherException : platform.lib.MyIllegalStateException()

fun test() {
    cancel(MyCancelException()) // TYPE_MISMATCH

    other(OtherException())
}
