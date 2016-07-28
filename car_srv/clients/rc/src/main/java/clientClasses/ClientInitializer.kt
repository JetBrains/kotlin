package client

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpClientCodec

/**
 * Created by user on 7/8/16.
 */
class ClientInitializer : ChannelInitializer<SocketChannel> {

    constructor()

    override fun initChannel(ch: SocketChannel) {
        val p = ch.pipeline()
        p.addLast(HttpClientCodec())
        p.addLast(ClientHandler())
    }
}