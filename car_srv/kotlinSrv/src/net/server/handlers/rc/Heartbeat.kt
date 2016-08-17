package net.server.handlers.rc

import exceptions.RcControlException
import net.server.handlers.AbstractHandler
import CodedInputStream
import encodeProtoBuf

/**
 * Created by user on 7/27/16.
 */
class Heartbeat : AbstractHandler {

    val fromServerObjectBuilder: HeartBeatRequest.BuilderHeartBeatRequest
    val toServerObjectBuilder: HeartBeatResponse.BuilderHeartBeatResponse

    constructor(fromSrv: HeartBeatRequest.BuilderHeartBeatRequest, toSrv: HeartBeatResponse.BuilderHeartBeatResponse) : super() {
        this.fromServerObjectBuilder = fromSrv
        this.toServerObjectBuilder = toSrv
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val message = fromServerObjectBuilder.build()
        message.mergeFrom(CodedInputStream(data))
        val resultCode: Int
        try {
            MicroController.instance.rcHeartBeat(message.sid)
            resultCode = 0
        } catch (e: RcControlException) {
            resultCode = 12
        }
        val responseMessage = toServerObjectBuilder.setCode(resultCode).build()
        callback.invoke(encodeProtoBuf(responseMessage))
    }
}