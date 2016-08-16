package net.server.handlers.rc

import MicroController
import carControl.RouteExecutorImpl.MoveDirection
import exceptions.RcControlException
import net.server.handlers.AbstractHandler
import DirectionRequest
import CodedInputStream
import CodedOutputStream

/**
 * Created by user on 7/27/16.
 */
class Control : AbstractHandler {

    val fromServerObjectBuilder: DirectionRequest.BuilderDirectionRequest
    val toServerObjectBuilder: DirectionResponse.BuilderDirectionResponse

    constructor(fromSrv: DirectionRequest.BuilderDirectionRequest, toSrv: DirectionResponse.BuilderDirectionResponse) : super() {
        this.fromServerObjectBuilder = fromSrv
        this.toServerObjectBuilder = toSrv
    }

    override fun getBytesResponse(data: ByteArray, callback: (b: ByteArray) -> Unit) {
        val message = fromServerObjectBuilder.build()
        message.mergeFrom(CodedInputStream(data))
        val commandNumber = message.command
        val sid = message.sid
        val command = when (commandNumber) {
            DirectionRequest.Command.stop -> {
                MoveDirection.STOP
            }
            DirectionRequest.Command.forward -> {
                MoveDirection.FORWARD
            }
            DirectionRequest.Command.backward -> {
                MoveDirection.BACKWARD
            }
            DirectionRequest.Command.right -> {
                MoveDirection.RIGHT
            }
            DirectionRequest.Command.left -> {
                MoveDirection.LEFT
            }
            else -> MoveDirection.STOP
        }
        val resultCode: Int
        try {
            MicroController.instance.RcMove(command, sid)
            resultCode = 0
        } catch (e: RcControlException) {
            resultCode = 12
        }
        val resultMessage = toServerObjectBuilder.setCode(resultCode).build()
        val resultByteArray = ByteArray(resultMessage.getSizeNoTag())
        resultMessage.writeTo(CodedOutputStream(resultByteArray))
        callback.invoke(resultByteArray)
    }
}