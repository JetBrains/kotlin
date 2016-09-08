package net

import CodedInputStream
import CodedOutputStream
import ConnectionRequest
import ConnectionResponse
import config
import mainServerPort
import require

class Client() {

    private val http = require("http")
    private var uid: Int = -1

    fun sendRequest(dataBuffer: dynamic, url: String, successCallback: (responseData: ByteArray) -> Unit, errorCallback: (err: dynamic) -> Unit) {
        val options: dynamic = {}
        //serverAddress, port and url used on js function. Idea don's see this
        val serverAddress = config.getIp()
        val port = mainServerPort
        js("options = {hostname:serverAddress, port:port, path:url, method:'POST'}")

        val request = http.request(options, { response ->
            val resData = mutableListOf<Byte>()
            response.on("data", { data ->
                for (i in 0..data.length - 1) {
                    resData.add(data[i])
                }
            })
            response.on("end", {
                successCallback.invoke(resData.toByteArray())
            })
        })
        request.on("error", { err ->
            errorCallback.invoke(err)
        })
        request.write(dataBuffer)
        request.end()
    }

    fun connectToServer(thisIp: String, thisPort: Int) {
        val requestObject = ConnectionRequest.BuilderConnectionRequest(thisIp.split(".").map { str -> parseInt(str, 10) }.toIntArray(), thisPort).build()
        val bytes = ByteArray(requestObject.getSizeNoTag())
        requestObject.writeTo(CodedOutputStream(bytes))
        sendRequest(js("new Buffer(bytes)"), "/connect", { resultData ->
            val responseObject = ConnectionResponse.BuilderConnectionResponse(0, 0).build()
            responseObject.mergeFrom(CodedInputStream(resultData))
            if (responseObject.code == 0) {
                this.uid = responseObject.uid
            } else {
                println("server login error\n" +
                        "code: ${responseObject.code}")
            }
        }, { error ->
            println("connection error (to main server). error message:\n" + error)
        })
    }

    companion object {
        val instance = Client()
    }
}