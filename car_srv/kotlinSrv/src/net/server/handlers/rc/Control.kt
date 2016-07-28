package net.server.handlers.rc

import MicroController
import carControl.RouteExecutorImpl.MoveDirection
import exceptions.RcControlException
import net.server.handlers.AbstractHandler

/**
 * Created by user on 7/27/16.
 */
class Control : AbstractHandler {

    constructor(protoDecoder: dynamic, protoEncoder: dynamic) : super(protoDecoder, protoEncoder)

    override fun makeResponse(message: dynamic, responseMessage: dynamic, finalCallback: () -> Unit) {
        val commandNumber = message.command
        val sid = message.sid
        val command = when (commandNumber) {
            protoDecoder.Command.stop -> {
                MoveDirection.STOP
            }
            protoDecoder.Command.forward -> {
                MoveDirection.FORWARD
            }
            protoDecoder.Command.backward -> {
                MoveDirection.BACKWARD
            }
            protoDecoder.Command.right -> {
                MoveDirection.RIGHT
            }
            protoDecoder.Command.left -> {
                MoveDirection.LEFT
            }
            else -> MoveDirection.STOP
        }
        val resultCode:Int
        val resultMsg:String
        try {
            MicroController.instance.RcMove(command, sid)
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