package net.server.handlers.rc

import exceptions.RcControlException
import net.server.handlers.AbstractHandler
import CodedOutputStream

/**
 * Created by user on 7/27/16.
 */
class Connect : AbstractHandler {

    val toServerObjectBuilder: SessionUpResponse.BuilderSessionUpResponse

    constructor(toSrv: SessionUpResponse.BuilderSessionUpResponse) : super() {
        this.toServerObjectBuilder = toSrv
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val resultCode: Int
        val sid: Int;
        try {
            sid = MicroController.instance.connectRC()
            resultCode = 0
        } catch (e: RcControlException) {
            resultCode = 13
            sid = 0
        }

        val responseMessage = toServerObjectBuilder.setCode(resultCode).setSid(sid).build()
        val resultByteArray = ByteArray(responseMessage.getSizeNoTag())
        responseMessage.writeTo(CodedOutputStream(resultByteArray))
        callback.invoke(resultByteArray)
    }

}