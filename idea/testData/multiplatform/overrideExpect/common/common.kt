expect class Expect

interface Base {
    fun expectInReturnType(): Expect

    fun expectInArgument(e: Expect)

    fun Expect.expectInReceiver()
}