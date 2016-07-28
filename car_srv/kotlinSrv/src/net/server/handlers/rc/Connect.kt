package net.server.handlers.rc

import exceptions.RcControlException
import net.server.handlers.AbstractHandler

/**
 * Created by user on 7/27/16.
 */
class Connect : AbstractHandler {

    constructor(protoDecoder: dynamic, protoEncoder: dynamic) : super(protoDecoder, protoEncoder)

    override fun makeResponse(message: dynamic, responseMessage: dynamic, finalCallback: () -> Unit) {
        val resultCode: Int
        val resultMsg: String
        val sid: Int;
        try {
            sid = MicroController.instance.connectRC()
            resultCode = 0
            resultMsg = ""
        } catch (e: RcControlException) {
            resultCode = 13
            resultMsg = "car already controlled by RC"
            sid = 0
        }

        responseMessage.sid = sid
        responseMessage.code = resultCode
        responseMessage.errorMsg = resultMsg

        finalCallback.invoke()
    }
}