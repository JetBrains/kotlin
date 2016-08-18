package net.server.handlers.flash

import net.server.handlers.AbstractHandler
import require
import CodedInputStream
import encodeProtoBuf
import mcTransport

/**
 * Created by user on 7/27/16.
 */
class LoadBin : AbstractHandler {

    val exec: dynamic

    val fromServerObjectBuilder: Upload.BuilderUpload
    val toServerObjectBuilder: UploadResult.BuilderUploadResult

    constructor(fromSrv: Upload.BuilderUpload, toSrv: UploadResult.BuilderUploadResult) : super() {
        this.fromServerObjectBuilder = fromSrv
        this.toServerObjectBuilder = toSrv
        this.exec = require("child_process").exec
    }

    override fun getBytesResponse(data: ByteArray, callback: (b: ByteArray) -> Unit) {
        val message = fromServerObjectBuilder.build()
        message.mergeFrom(CodedInputStream(data))
        val responseMessage = toServerObjectBuilder.build()
        mcTransport.sendBytes(message.data)
        val stFlashCommand = "./st-flash write ./flash.bin " + "0x08000000"
        exec(stFlashCommand, { err, stdOutRes, stdErrRes ->
            val resultCode = if (err != null) 15 else 0
            responseMessage.resultCode = resultCode
            callback.invoke(encodeProtoBuf(responseMessage))
        })
    }
}