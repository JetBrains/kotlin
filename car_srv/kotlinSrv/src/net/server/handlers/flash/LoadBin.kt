package net.server.handlers.flash

import net.server.handlers.AbstractHandler
import require
import CodedInputStream
import CodedOutputStream
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
        var resultCode = 0
        val responseMessage = toServerObjectBuilder.build()
        mcTransport.writeToFile(message.data)
//        if (error != null) {
//            resultCode = 14
//            responseMessage.resultCode = resultCode
//            val resultByteArray = ByteArray(responseMessage.getSizeNoTag())
//            responseMessage.writeTo(CodedOutputStream(resultByteArray))
//            callback.invoke(resultByteArray)
//        } else {
        val stFlashCommand = "./st-flash write ./flash.bin " + "0x08000000"
        exec(stFlashCommand, { err, stdOutRes, stdErrRes ->
            if (err != null) {
                resultCode = 15
            } else {
                resultCode = 0
            }
            responseMessage.resultCode = resultCode
            val resultByteArray = ByteArray(responseMessage.getSizeNoTag())
            responseMessage.writeTo(CodedOutputStream(resultByteArray))
            callback.invoke(resultByteArray)
        })
//        }
    }
}