package net.server.handlers.flash

import CodedInputStream
import Upload
import UploadResult
import encodeProtoBuf
import mcTransport
import net.server.handlers.AbstractHandler
import require

/**
 * Created by user on 7/27/16.
 */
class LoadBin : AbstractHandler {

    val exec: dynamic

    val fromServerObjectBuilder: Upload.BuilderUpload
    val toServerObjectBuilder: UploadResult.BuilderUploadResult

    constructor() : super() {
        this.fromServerObjectBuilder = Upload.BuilderUpload(ByteArray(0))
        this.toServerObjectBuilder = UploadResult.BuilderUploadResult(0)
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