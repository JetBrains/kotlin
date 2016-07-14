package client

import com.google.protobuf.InvalidProtocolBufferException
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpContent
import proto.Carkot
import java.util.*

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

        var resultCode = 0
        try {
            val uploadResult: Carkot.UploadResult = Carkot.UploadResult.parseFrom(contentBytes)
            resultCode = uploadResult.resultCode
        } catch (e: InvalidProtocolBufferException) {
            e.printStackTrace()

            resultCode = 2
        }
        synchronized(requestResult, {
            requestResult.code = resultCode
        })
        ctx.close()
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is HttpContent) {
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