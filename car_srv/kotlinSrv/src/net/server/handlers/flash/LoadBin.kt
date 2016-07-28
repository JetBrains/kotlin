package net.server.handlers.flash

import net.server.handlers.AbstractHandler
import require

/**
 * Created by user on 7/27/16.
 */
class LoadBin : AbstractHandler {

    val fs: dynamic
    val exec: dynamic

    constructor(protoDecoder: dynamic, protoEncoder: dynamic) : super(protoDecoder, protoEncoder) {
        this.fs = require("fs")
        this.exec = require("child_process").exec
    }

    override fun makeResponse(message: dynamic, responseMessage: dynamic, finalCallback: () -> Unit) {
        var stdOut: String = ""
        var strErr: String = ""
        var resultCode = 0
        fs.writeFile("./flash.bin", message.data.buffer, "binary", { error ->
            if (error != null) {
                resultCode = 14
                strErr = error.toString()
                responseMessage.stdOut = stdOut
                responseMessage.strErr = strErr
                responseMessage.resultCode = resultCode
                finalCallback.invoke()
            } else {
                val stFlashCommand = "./st-flash write ./flash.bin " + message.base
                exec(stFlashCommand, { err, stdOutRes, stdErrRes ->
                    if (err != null) {
                        resultCode = 15
                        strErr = stdErrRes.toString() + "\n" + err.toString()
                        stdOut = stdOutRes.toString()
                    } else {
                        resultCode = 0
                        strErr = stdErrRes.toString()
                        stdOut = stdOutRes.toString()
                    }
                    responseMessage.stdOut = stdOut
                    responseMessage.strErr = strErr
                    responseMessage.resultCode = resultCode
                    finalCallback.invoke()
                })
            }
        })
    }
}