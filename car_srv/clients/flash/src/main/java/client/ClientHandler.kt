package client

import com.google.protobuf.InvalidProtocolBufferException
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpContent

/**
 * Created by user on 7/8/16.
 */
class ClientHandler : SimpleChannelInboundHandler<Any> {

    object requestResult {
        var code: Int = -1
        var stdOut: String = ""
        var stdErr: String = ""
    }

    constructor()

    var contentBytes: ByteArray = ByteArray(0);

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        val resultCode:Int
        val resultStdOut:String
        val resultStdErr:String
        try {
//            val uploadResult: Carkot.UploadResult = Carkot.UploadResult.parseFrom(contentBytes)
//            resultCode = uploadResult.resultCode
//            resultStdOut = uploadResult.stdOut
//            resultStdErr = uploadResult.stdErr
        } catch (e: InvalidProtocolBufferException) {
            e.printStackTrace()
            resultStdErr = ""
            resultStdOut = ""
            resultCode = 2
        }
        synchronized(requestResult, {
//            requestResult.code = resultCode
//            requestResult.stdErr = resultStdErr
//            requestResult.stdOut = resultStdOut
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