package client

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.handler.codec.http.HttpContent

/**
 * Created by user on 7/8/16.
 */
class ClientHandler : SimpleChannelInboundHandler<Any> {

    object requestResult {
        var code: Int = -1
    }

    constructor()

    var contentBytes: ByteArray = ByteArray(0);

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        if (contentBytes.size == 0) {
            ctx.close()
            return
        }
        val resultCode: Int = contentBytes[0].toInt()
        synchronized(requestResult, {
            requestResult.code = resultCode
        })
        ctx.close()
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is DefaultHttpContent) {
            val contentsBytes = msg.content();
            contentBytes = ByteArray(contentsBytes.capacity())
            contentsBytes.readBytes(contentBytes)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable) {
        cause.printStackTrace()
        if (ctx != null) {
            ctx.close()
        }
    }
}