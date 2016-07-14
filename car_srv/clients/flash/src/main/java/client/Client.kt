package client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFutureListener
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AttributeKey
import java.net.ConnectException

/**
 * Created by user on 7/8/16.
 */
object Client {

    fun sendRequest(request: HttpRequest, host: String, port: Int):Int {
        val group = NioEventLoopGroup()
        try {
            val bootstrap: Bootstrap = Bootstrap()
            bootstrap.group(group).channel(NioSocketChannel().javaClass).handler(ClientInitializer())
            val channelFuture = bootstrap.connect(host, port).sync()
            val channel = channelFuture.channel()
            channel.writeAndFlush(request)
            channel.closeFuture().sync()
        } catch (e: InterruptedException) {
            println("interrupted before request done")
            return 2
        } catch (e: ConnectException) {
            println("connection error to $host:$port")
            return 1
        } finally {
            group.shutdownGracefully()
        }
        return ClientHandler.requestResult.code
    }

}