import com.beust.jcommander.JCommander
import com.google.protobuf.ByteString
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import proto.Carkot
import java.util.*

/**
 * Created by user on 7/11/16.
 */

fun main(args: Array<String>) {
    //2 argm path to bin and server ip
    //1 option - 0x000...
    var asss:ArrayList<String> = ArrayList();

    val clArgs:CommandLineArgs = CommandLineArgs();
    val jcom = JCommander(clArgs)

    val host = "127.0.0.1"
    val port = 8888
    val base = "0x08000000"
//    val client = client.Client
    val bytesBinTest = Carkot.Upload.newBuilder().setData(ByteString.copyFrom(ByteArray(5, {x->0}))).setBase(base).build().toByteArray()

    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/loadBin", Unpooled.copiedBuffer(bytesBinTest));
    request.headers().set(HttpHeaderNames.HOST, host)
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

    client.Client.sendRequest(request, host, port)
    println(client.ClientHandler.requestResult.code)
}
