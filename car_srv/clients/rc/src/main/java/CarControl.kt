import client.Client
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import proto.car.RouteP
import proto.car.RouteP.Direction.Command

/**
 * Created by user on 7/14/16.
 */
class CarControl constructor(client: Client) {

    val client: Client

    init {
        this.client = client
    }

    fun executeCommand(direction: Char) {

        val directionBuilder = RouteP.Direction.newBuilder()
        when (direction) {
            'w' -> {
                directionBuilder.setCommand(Command.forward)
            }
            's' -> {
                directionBuilder.setCommand(Command.backward)
            }
            'd' -> {
                directionBuilder.setCommand(Command.right)
            }
            'a' -> {
                directionBuilder.setCommand(Command.left)
            }
            'h' -> {
                directionBuilder.setCommand(Command.stop)
            }
        }
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/control", Unpooled.copiedBuffer(directionBuilder.build().toByteArray()));
        request.headers().set(HttpHeaderNames.HOST, client.host)
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

        client.sendRequest(request)
    }
}


