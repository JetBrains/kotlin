import client.Client
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import java.io.ByteArrayOutputStream

/**
 * Created by user on 7/14/16.
 */
class CarControl constructor(client: Client) {

    val client: Client

    init {
        this.client = client
    }

    fun executeCommand(command: DirectionRequest.Command) {

        val directionBuilder = DirectionRequest.BuilderDirectionRequest()
        directionBuilder.setCommand(command)
        val byteArrayStream = ByteArrayOutputStream()

        directionBuilder.build().writeTo(CodedOutputStream(byteArrayStream))
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/control", Unpooled.copiedBuffer(byteArrayStream.toByteArray()));
        request.headers().set(HttpHeaderNames.HOST, client.host)
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

        client.sendRequest(request)
    }
}


