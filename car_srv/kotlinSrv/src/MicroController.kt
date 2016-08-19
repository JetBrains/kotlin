import control.emulator.RouteExecutorImpl
import control.car.RouteExecutorToUsb
import exceptions.RcControlException
import kotlin.js.Date

/**
 * Created by user on 7/28/16.
 */
class MicroController private constructor() {

    var uid: Int

    var rcSid: Int
    var rcLastRequest: Int//heartbeat

    val vendorID: String
    val modelID: String
    var transportFilePath: String

    val car: CarState

    init {
        this.rcSid = 0
        this.rcLastRequest = 0
        this.uid = 0
        this.vendorID = "0483"
        this.modelID = "5740"
        this.transportFilePath = ""


        this.car = CarState.instance
    }

    fun start() {

        connectToServer(config.getCarIp(), carServerPort)

        mcTransport.setCallBack { bytes ->
            println("read: " + bytes.toString())
        }

//        setInterval({ this.car.refreshLocation(deltaTimeLocationRefresh) }, deltaTimeLocationRefresh);
    }

    fun connectToServer(thisIp: String, thisPort: Int) {
        val requestObject = ConnectionRequest.BuilderConnectionRequest(thisIp.split(".").map { str -> parseInt(str, 10) }.toIntArray(), thisPort).build()
        val bytes = ByteArray(requestObject.getSizeNoTag())
        requestObject.writeTo(CodedOutputStream(bytes))
        net.sendRequest(js("new Buffer(bytes)"), "/connect", { resultData ->
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
        val instance = MicroController()
    }

}
