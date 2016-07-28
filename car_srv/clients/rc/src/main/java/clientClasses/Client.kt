package clientClasses

import io.netty.bootstrap.Bootstrap
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AttributeKey
import java.net.ConnectException

/**
 * Created by user on 7/8/16.
 */
class Client constructor(host: String, port: Int) {

    val host: String
    val port: Int
    val group: EventLoopGroup


    init {
        this.host = host
        this.port = port
        group = NioEventLoopGroup(1)
    }

    val bootstrap: Bootstrap = makeBootstrap()

    private fun makeBootstrap(): Bootstrap {
        val b = Bootstrap()
        b.group(group).channel(NioSocketChannel().javaClass).handler(ClientInitializer())
                .attr(AttributeKey.newInstance<String>("url"), "")
        return b
    }

    fun sendRequest(request: HttpRequest) {
        try {
            bootstrap.attr(AttributeKey.valueOf<String>("url"), request.uri())
            val channelFuture = bootstrap.connect(host, port).sync()
            val channel = channelFuture.channel()
            channel.writeAndFlush(request)
            channel.closeFuture().sync()
        } catch (e: InterruptedException) {
            ClientHandler.requestResult.code = 11
            ClientHandler.requestResult.errorString = "command execution interrupted"
        } catch (e: ConnectException) {
            ClientHandler.requestResult.code = 10
            ClientHandler.requestResult.errorString = "don't can connect to server ($host:$port)"
        }
    }

    fun close() {
        group.shutdownGracefully()
    }

}