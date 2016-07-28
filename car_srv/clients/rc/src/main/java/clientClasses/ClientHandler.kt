package clientClasses

import CodedInputStream
import DirectionResponse
import InvalidProtocolBufferException
import connectUrl
import controlUrl
import disconnectUrl
import heartbeatUrl
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.util.AttributeKey
import java.io.ByteArrayInputStream

/**
 * Created by user on 7/8/16.
 */
class ClientHandler : SimpleChannelInboundHandler<Any> {

    object requestResult {
        var code = -1
        var errorString = ""
        var sid = 0
    }

    constructor()

    var contentBytes: ByteArray = ByteArray(0);

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        if (!ctx.channel().isOpen) {
            ctx.close()
            return
        }
        val url = ctx.channel().attr(AttributeKey.valueOf<String>("url")).get()
        val responseStream = CodedInputStream(ByteArrayInputStream(contentBytes))
        when (url) {
            controlUrl -> {
                val response = DirectionResponse.BuilderDirectionResponse().build()
                try {
                    response.mergeFrom(responseStream)
                    setRequestResultValues(response.code, response.errorMsg)
                } catch (e: InvalidProtocolBufferException) {
                    setRequestResultValues(1, "protobuf parsing error. bytes from server is not message. stack trace:\n ${e.message}")
                }
            }
            connectUrl -> {
                val response = SessionUpResponse.BuilderSessionUpResponse().build()
                try {
                    response.mergeFrom(responseStream)
                    setRequestResultValues(response.code, response.errorMsg, response.sid)
                } catch (e: InvalidProtocolBufferException) {
                    setRequestResultValues(1, "protobuf parsing error. bytes from server is not message. stack trace:\n ${e.message}", response.sid)
                }
            }
            disconnectUrl -> {
                val response = SessionDownResponse.BuilderSessionDownResponse().build()
                try {
                    response.mergeFrom(responseStream)
                    setRequestResultValues(response.code, response.errorMsg)
                } catch (e: InvalidProtocolBufferException) {
                    setRequestResultValues(1, "protobuf parsing error. bytes from server is not message. stack trace:\n ${e.message}")
                }
            }
            heartbeatUrl -> {
                val response = HeartBeatResponse.BuilderHeartBeatResponse().build()
                try {
                    response.mergeFrom(responseStream)
                    setRequestResultValues(response.code, response.errorMsg)
                } catch (e: InvalidProtocolBufferException) {
                    setRequestResultValues(1, "protobuf parsing error. bytes from server is not message. stack trace:\n ${e.message}")
                }
            }
        }
        ctx.close()
    }

    fun setRequestResultValues(code: Int, error: String, sid: Int = -1) {
        synchronized(requestResult, {
            requestResult.code = code
            requestResult.errorString = error
            if (sid != -1) {
                requestResult.sid = sid
            }
        })
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