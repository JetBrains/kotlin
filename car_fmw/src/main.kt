
fun writeProto() {
    val msg = DirectionResponse.BuilderDirectionResponse(4242).build()
//    val size = msg.getSizeNoTag()
//    val buffer = ByteArray(size)
//    val outputStream = CodedOutputStream(buffer)
//
//    msg.writeTo(outputStream)
//    sendByteArray(buffer)
}

fun main() {
    init()
    writeProto()
    simpleRoute()
}
