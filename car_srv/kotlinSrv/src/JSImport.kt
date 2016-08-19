/**
 * Created by user on 8/18/16.
 */

@native
fun require(name: String): dynamic = noImpl

@native
fun setInterval(callBack: () -> Unit, ms: Int): dynamic = noImpl

@native
fun setTimeout(callBack: () -> Unit, ms: Int): dynamic = noImpl

fun encodeProtoBuf(protoMessage: dynamic): ByteArray {
    val protoSize = protoMessage.getSizeNoTag()
    val routeBytes = ByteArray(protoSize)

    protoMessage.writeTo(CodedOutputStream(routeBytes))
    return routeBytes
}