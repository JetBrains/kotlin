import exceptions.RcControlException
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
val serverIp: String = "127.0.0.1"

val protoBuf = require("protobufjs")
fun main(args: Array<String>) {

    val controlConstructor = protoBuf.loadProtoFile(getRelativePathToProto("direction.proto")).build("carkot")
    val rcSessionConstructor = protoBuf.loadProtoFile(getRelativePathToProto("rc_session.proto")).build("carkot")
    val carkotConstructor = protoBuf.loadProtoFile(getRelativePathToProto("carkot.proto")).build("carkot")

    val routeConstructor = protoBuf.loadProtoFile(getRelativePathToProto("route.proto")).build("carkot")
    val locationConstructor = protoBuf.loadProtoFile(getRelativePathToProto("location.proto")).build("carkot")


    val handlers: MutableMap<String, AbstractHandler> = mutableMapOf()
    handlers.put("/rc/control", Control(controlConstructor.DirectionRequest, controlConstructor.DirectionResponse))
    handlers.put("/rc/connect", Connect(null, rcSessionConstructor.SessionUpResponse))
    handlers.put("/rc/disconnect", Disconnect(rcSessionConstructor.SessionDownRequest, rcSessionConstructor.SessionDownResponse))
    handlers.put("/rc/heartbeat", Heartbeat(rcSessionConstructor.HeartBeatRequest, rcSessionConstructor.HeartBeatResponse))

    handlers.put("/loadBin", LoadBin(carkotConstructor.Upload, carkotConstructor.UploadResult))

    handlers.put("/route", SetRoute(routeConstructor.RouteRequest, routeConstructor.RouteResponse))
    handlers.put("/getLocation", GetLocation(null, locationConstructor.LocationResponse))

    net.server.start(handlers, serverPort)
    val udev = Udev()
    udev.start()

    MicroController.instance.start()

}

fun getRelativePathToProto(fileName: String): String {
    return "./proto/" + fileName
}

fun trimBuffer(buffer: dynamic, length: Int): dynamic {
    val byteArray = ByteArray(length);
    for (i in 0..length - 1) {
        byteArray[i] = buffer[i]
    }
    return js("new Buffer(byteArray)")
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