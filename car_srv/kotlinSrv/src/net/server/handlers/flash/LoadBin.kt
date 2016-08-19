package net.server.handlers.flash

import CodedInputStream
import Upload
import UploadResult
import encodeProtoBuf
import fs
import net.server.handlers.AbstractHandler
import require

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
        fs.writeFile("./flash.bin", js("new Buffer(data)"), { err, stdOut, stdErr ->
            if (err) {
                println("error in save flash.bin file\n $err")
                val responseMessage = toServerObjectBuilder.setResultCode(14).build()
                callback.invoke(encodeProtoBuf(responseMessage))
            }
            val stFlashCommand = "./st-flash write ./flash.bin " + "0x08000000"
            exec(stFlashCommand, { err, stdOutRes, stdErrRes ->
                val resultCode = if (err != null) 15 else 0
                val responseMessage = toServerObjectBuilder.setResultCode(resultCode).build()
                callback.invoke(encodeProtoBuf(responseMessage))
            })
        })
    }
}