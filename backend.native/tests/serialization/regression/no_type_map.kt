import kotlinx.cinterop.*
fun main(args: Array<String>) {
    memScoped {
        val bufferLength = 100L
        val buffer = allocArray<ByteVar>(bufferLength)
    }
    println("OK")
}
