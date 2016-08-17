import net.server.handlers.AbstractHandler
import net.server.handlers.flash.LoadBin
import net.server.handlers.main.GetLocation
import net.server.handlers.main.SetRoute
import net.server.handlers.rc.Connect
import net.server.handlers.rc.Control
import net.server.handlers.rc.Disconnect
import net.server.handlers.rc.Heartbeat

/**
 * Created by user on 7/26/16.
 */

val deltaTimeLocationRefresh: Int = 100//ms

val serverPort: Int = 8888
val mainServerPort = 7925

val config = Config("config.cfg")
val fs = require("fs")
fun main(args: Array<String>) {
    if (!config.loadConfig()) {
        println("incorrect config format!")
        return
    }

    val handlers: MutableMap<String, AbstractHandler> = mutableMapOf()
    handlers.put("/rc/control", Control(DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.fromIntToCommand(0), 0), DirectionResponse.BuilderDirectionResponse(0)))
    handlers.put("/rc/connect", Connect(SessionUpResponse.BuilderSessionUpResponse(0, 0)))
    handlers.put("/rc/disconnect", Disconnect(SessionDownRequest.BuilderSessionDownRequest(0), SessionDownResponse.BuilderSessionDownResponse(0)))
    handlers.put("/rc/heartbeat", Heartbeat(HeartBeatRequest.BuilderHeartBeatRequest(0), HeartBeatResponse.BuilderHeartBeatResponse(0)))

    handlers.put("/loadBin", LoadBin(Upload.BuilderUpload(ByteArray(0)), UploadResult.BuilderUploadResult(0)))

    handlers.put("/route", SetRoute(RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0)), RouteResponse.BuilderRouteResponse(0)))
    handlers.put("/getLocation", GetLocation(LocationResponse.BuilderLocationResponse(LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build(), 0)))

    net.server.start(handlers, serverPort)
    val udev = Udev()
    udev.start()

    MicroController.instance.start()

}

fun encodeInt(i: Int): ByteArray {
    val result = ByteArray(4)
    result[0] = i.shr(24).toByte()
    result[1] = i.shr(16).toByte()
    result[2] = i.shr(8).toByte()
    result[3] = i.toByte()

    return result
}

fun decodeInt(bytes: ByteArray): Int {
    var result = 0
    result += bytes[3]
    result += bytes[2].toInt().shl(8)
    result += bytes[1].toInt().shl(16)
    result += bytes[0].toInt().shl(24)
    return result
}

fun encodeProtoBuf(protoMessage: dynamic): ByteArray {
    val protoSize = protoMessage.getSizeNoTag()
    val routeBytes = ByteArray(protoSize)

    protoMessage.writeTo(CodedOutputStream(routeBytes))
    return routeBytes
}

@native
fun require(name: String): dynamic {
    return null
}

@native
fun setInterval(callBack: () -> Unit, ms: Int) {

}

@native
fun setTimeout(callBack: () -> Unit, ms: Int) {

}