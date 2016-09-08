package net.server.handlers.rc

import DirectionRequest
import net.server.handlers.AbstractHandler

class Control() : AbstractHandler() {

    val request = DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.fromIntToCommand(0), 0, false)

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
        callback.invoke(ByteArray(0))
    }
}