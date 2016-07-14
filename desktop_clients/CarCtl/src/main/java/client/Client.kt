package client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpRequest
import java.net.ConnectException

/**
 * Created by user on 7/8/16.
 */
class Client constructor(host: String, port: Int) {

    val host: String
    val port: Int

    init {
        this.host = host
        this.port = port
    }

    fun sendRequest(request: HttpRequest): Int {
        val group = NioEventLoopGroup(1)
        try {
            val bootstrap = Bootstrap()
            bootstrap.group(group).channel(NioSocketChannel().javaClass).handler(ClientInitializer())
            val channelFuture = bootstrap.connect(host, port).sync()
            val channel = channelFuture.channel()
            channel.writeAndFlush(request)
            channel.closeFuture().sync()
        } catch (e: InterruptedException) {
            println("interrupted before request done")
            return 2
        } catch (e: ConnectException) {
            println("connection error")
            return 1
        } finally {
            group.shutdownGracefully()
        }
        return ClientHandler.requestResult.code
    }

}