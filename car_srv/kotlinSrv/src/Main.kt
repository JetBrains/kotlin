import carControl.ControlImpl
import carControl.RouteExecutorImpl
import server.Server
import server.handlers.AbstractHandler
import server.handlers.flash.LoadBin
import server.handlers.rc.Control

/**
 * Created by user on 7/26/16.
 */

var transportFilePath: String = "./test"//todo init on start after start udev
val thisCar = Car(RouteExecutorImpl(), ControlImpl())
val deltaTimeLocationRefresh: Int = 100//ms

fun main(args: Array<String>) {

    val protoBuf = require("protobufjs")
    val controlConstructor = protoBuf.loadProtoFile("./proto/" + "direction.proto").build("carkot")
    val handlers: MutableMap<String, AbstractHandler> = mutableMapOf()
    handlers.put("/control", Control(controlConstructor.DirectionRequest, controlConstructor.DirectionResponse))

    val srv = Server(handlers)
    srv.start()


    setInterval({ thisCar.refreshLocation(deltaTimeLocationRefresh) }, deltaTimeLocationRefresh);
}

@native
fun require(name: String): dynamic {
    return null
}

@native
fun setInterval(callBack: () -> Unit, ms: Int) {

}