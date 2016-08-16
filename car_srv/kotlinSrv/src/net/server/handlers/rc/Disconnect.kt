package net.server.handlers.rc

import exceptions.RcControlException
import net.server.handlers.AbstractHandler
import CodedInputStream
import CodedOutputStream

/**
 * Created by user on 7/27/16.
 */
class Disconnect : AbstractHandler {

    val fromServerObjectBuilder: SessionDownRequest.BuilderSessionDownRequest
    val toServerObjectBuilder: SessionDownResponse.BuilderSessionDownResponse

    constructor(fromSrv: SessionDownRequest.BuilderSessionDownRequest, toSrv: SessionDownResponse.BuilderSessionDownResponse) : super() {
        this.fromServerObjectBuilder = fromSrv
        this.toServerObjectBuilder = toSrv
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {

        val message = fromServerObjectBuilder.build()
        message.mergeFrom(CodedInputStream(data))
        val resultCode: Int
        try {
            MicroController.instance.disconnectRC(message.sid)
            resultCode = 0
        } catch (e: RcControlException) {
            resultCode = 12
        }
        val responseMessage = toServerObjectBuilder.setCode(resultCode).build()
        val resultByteArray = ByteArray(responseMessage.getSizeNoTag())
        responseMessage.writeTo(CodedOutputStream(resultByteArray))
        callback.invoke(resultByteArray)
    }
}