package net.server.handlers.rc

import CodedInputStream
import DirectionRequest
import DirectionResponse
import control.emulator.ControllerEmulator.MoveDirection
import encodeProtoBuf
import net.server.handlers.AbstractHandler

class Control : AbstractHandler {

    val fromServerObjectBuilder: DirectionRequest.BuilderDirectionRequest
    val toServerObjectBuilder: DirectionResponse.BuilderDirectionResponse

    constructor() : super() {
        this.fromServerObjectBuilder = DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.fromIntToCommand(0), 0)
        this.toServerObjectBuilder = DirectionResponse.BuilderDirectionResponse(0)
    }

    override fun getBytesResponse(data: ByteArray, callback: (b: ByteArray) -> Unit) {
        //TODO now this handler don't make nothing. need fix:)
//        val message = fromServerObjectBuilder.build()
//        message.mergeFrom(CodedInputStream(data))
//        val commandNumber = message.command
//        val sid = message.sid
//        val command = when (commandNumber) {
//            DirectionRequest.Command.stop -> {
//                MoveDirection.STOP
//            }
//            DirectionRequest.Command.forward -> {
//                MoveDirection.FORWARD
//            }
//            DirectionRequest.Command.backward -> {
//                MoveDirection.BACKWARD
//            }
//            DirectionRequest.Command.right -> {
//                MoveDirection.RIGHT
//            }
//            DirectionRequest.Command.left -> {
//                MoveDirection.LEFT
//            }
//            else -> MoveDirection.STOP
//        }
        val resultCode: Int = 0
        val resultMessage = toServerObjectBuilder.setCode(resultCode).build()
        callback.invoke(encodeProtoBuf(resultMessage))
    }
}