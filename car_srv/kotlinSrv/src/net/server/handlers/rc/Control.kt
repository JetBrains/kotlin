package server.handlers.rc

import carControl.RouteExecutorImpl
import server.handlers.AbstractHandler
import thisCar
import carControl.RouteExecutorImpl.MoveDirection

/**
 * Created by user on 7/27/16.
 */
class Control : AbstractHandler {

    constructor(protoDecoder: dynamic, protoEncoder: dynamic) : super(protoDecoder, protoEncoder)

    override fun makeResult(message: dynamic, resultMessage: dynamic, finalCallback: () -> Unit) {
        val commandNumber = message.command
        resultMessage.code = 0
        resultMessage.errorMsg = ""
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
        thisCar.move(command, 0.0, {})
        finalCallback.invoke()
    }
}