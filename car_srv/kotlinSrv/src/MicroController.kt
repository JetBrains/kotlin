import carControl.ControlImpl
import carControl.RouteExecutorImpl
import exceptions.RcControlException

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

    val car: Car

    init {
        this.rcSid = 0
        this.rcLastRequest = 0
        this.uid = 0
        this.vendorID = "0483"
        this.modelID = "5740"
        this.transportFilePath = ""

        this.car = Car(RouteExecutorImpl(), ControlImpl())
    }

    fun start() {

        if (!isConnected()) {
            connectToServer(serverIp, serverPort)
        }

        this.transportFilePath = "./temp"//todo need init

        setInterval({ this.car.refreshLocation(deltaTimeLocationRefresh) }, deltaTimeLocationRefresh);
        setInterval({
            if (needDropRC()) {
                dropRC()
            }
        }, 500)
    }

    fun dropRC() {
        this.rcSid = 0
        car.stopCar()
    }

    fun disconnectRC(sid: Int) {
        if (sid != rcSid) {
            throw RcControlException()
        }
        dropRC()
    }

    fun RcMove(command: RouteExecutorImpl.MoveDirection, sid: Int) {
        if (sid != this.rcSid) {
            throw RcControlException()
        }
        car.move(command, 0.0, {})
        rcHeartBeat(sid)
    }

    fun needDropRC(): Boolean {
        return Date().getTime() > (rcLastRequest + 1000) && controlledByRC()
    }

    fun connectRC(): Int {
        if (controlledByRC()) {
            throw RcControlException()
        }
        val sid = (Math.random() * 100000).toInt()
        this.rcSid = sid;
        rcHeartBeat(sid)
        return sid
    }

    fun controlledByRC(): Boolean {
        return (rcSid != 0)
    }

    fun isConnected(): Boolean {
        return uid != 0
    }

    fun rcHeartBeat(sid: Int) {
        if (sid != this.rcSid) {
            throw RcControlException()
        }
        rcLastRequest = Date().getTime()
    }

    fun connectToServer(thisIp: String, thisPort: Int) {
        val requestObject = ConnectionRequest.BuilderConnectionRequest(serverIp.split(".").map { str -> parseInt(str, 10) }.toIntArray(), serverPort).build()
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
