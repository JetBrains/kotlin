package net.server.handlers.rc

import exceptions.RcControlException
import net.server.handlers.AbstractHandler

/**
 * Created by user on 7/27/16.
 */
class Heartbeat : AbstractHandler {

    constructor(protoDecoder: dynamic, protoEncoder: dynamic) : super(protoDecoder, protoEncoder)

    override fun makeResponse(message: dynamic, responseMessage: dynamic, finalCallback: () -> Unit) {
        val resultCode: Int
        val resultMsg: String
        try {
            MicroController.instance.rcHeartBeat(message.sid)
            resultCode = 0
            resultMsg = ""
        } catch (e: RcControlException) {
            resultCode = 12
            resultMsg = "incorrect remote control sid"
        }
        responseMessage.code = resultCode
        responseMessage.errorMsg = resultMsg
        finalCallback.invoke()
    }
}