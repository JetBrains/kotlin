package client

import CodedInputStream
import DirectionResponse
import InvalidProtocolBufferException
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultHttpContent
import java.io.ByteArrayInputStream

/**
 * Created by user on 7/8/16.
 */
class ClientHandler : SimpleChannelInboundHandler<Any> {

    object requestResult {
        var code = -1
        var errorString = ""
    }

    constructor()

    var contentBytes: ByteArray = ByteArray(0);

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        if (contentBytes.size == 0) {
            //socket is closed
            ctx.close()
            return
        }
        val responseStream = CodedInputStream(ByteArrayInputStream(contentBytes))
        val response = DirectionResponse.BuilderDirectionResponse().build()
        try {
            response.mergeFrom(responseStream)
        } catch (e: InvalidProtocolBufferException) {
            synchronized(requestResult, {
                requestResult.code = 1
                requestResult.errorString = "protobuf parsing error. bytes from server is not message. stack trace:\n ${e.message}"
            })
            return
        }
        synchronized(requestResult, {
            requestResult.code = response.code
            requestResult.errorString = response.errorMsg
        })
        ctx.close()
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is DefaultHttpContent) {
            //read bytes from http body
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