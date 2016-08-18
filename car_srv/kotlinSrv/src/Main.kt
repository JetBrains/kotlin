import net.server.handlers.AbstractHandler
import net.server.handlers.debug.Memory
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

val carServerPort: Int = 8888
val mainServerPort = 7925

val config = Config("config.cfg")
val fs = require("fs")
fun main(args: Array<String>) {
    if (!config.loadConfig()) {
        println("incorrect config format!")
        js("process.exit(1)")
    }

    val handlers: MutableMap<String, AbstractHandler> = mutableMapOf()
    // TODO remove injection of all internal builders from here
    handlers.put("/rc/control", Control(DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.fromIntToCommand(0), 0), DirectionResponse.BuilderDirectionResponse(0)))
    handlers.put("/rc/connect", Connect(SessionUpResponse.BuilderSessionUpResponse(0, 0)))
    handlers.put("/rc/disconnect", Disconnect(SessionDownRequest.BuilderSessionDownRequest(0), SessionDownResponse.BuilderSessionDownResponse(0)))
    handlers.put("/rc/heartbeat", Heartbeat(HeartBeatRequest.BuilderHeartBeatRequest(0), HeartBeatResponse.BuilderHeartBeatResponse(0)))

    handlers.put("/loadBin", LoadBin(Upload.BuilderUpload(ByteArray(0)), UploadResult.BuilderUploadResult(0)))

    handlers.put("/route", SetRoute(RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0)), RouteResponse.BuilderRouteResponse(0)))
    handlers.put("/getLocation", GetLocation(LocationResponse.BuilderLocationResponse(LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build(), 0)))


    handlers.put("/debug/memoty", Memory())

    net.server.start(handlers, carServerPort)
    val udev = Udev()
    udev.start()

    MicroController.instance.start()

}

// TODO move this dump of arbitrary functions that don't have any relation to
// to Main.kt from Main.kt

fun encodeProtoBuf(protoMessage: dynamic): ByteArray {
    val protoSize = protoMessage.getSizeNoTag()
    val routeBytes = ByteArray(protoSize)

    protoMessage.writeTo(CodedOutputStream(routeBytes))
    return routeBytes
}

@native
fun require(name: String): dynamic = noImpl
@native
fun setInterval(callBack: () -> Unit, ms: Int) : dynamic = noImpl
@native
fun setTimeout(callBack: () -> Unit, ms: Int) : dynamic = noImpl