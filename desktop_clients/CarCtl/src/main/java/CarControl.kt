import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import proto.car.RouteP
import proto.car.RouteP.Direction.Command

/**
 * Created by user on 7/14/16.
 */
class CarControl constructor(host: String, port: Int) {

    val host: String;
    val port: Int

    init {
        this.host = host;
        this.port = port;
    }


    fun executeCommand(direction: Char) {

        val directionBuilder = RouteP.Direction.newBuilder()
        when (direction) {
            'f' -> {
                directionBuilder.setCommand(Command.forward)
            }
            'b' -> {
                directionBuilder.setCommand(Command.backward)
            }
            'r' -> {
                directionBuilder.setCommand(Command.right)
            }
            'l' -> {
                directionBuilder.setCommand(Command.left)
            }
            's' -> {
                directionBuilder.setCommand(Command.stop)
            }
        }
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/control", Unpooled.copiedBuffer(directionBuilder.build().toByteArray()));
        request.headers().set(HttpHeaderNames.HOST, host)
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

        client.Client.sendRequest(request, host, port)
    }
}


