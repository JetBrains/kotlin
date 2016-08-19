import control.car.RouteExecutorToUsb
import net.server.handlers.AbstractHandler
import net.server.handlers.debug.Memory
import net.server.handlers.flash.LoadBin
import net.server.handlers.main.GetLocation
import net.server.handlers.main.SetRoute
import net.server.handlers.rc.Control

/**
 * Created by user on 7/26/16.
 */

val deltaTimeLocationRefresh: Int = 100

val carServerPort: Int = 8888
val mainServerPort = 7925

val config = Config()
val fs = require("fs")
fun main(args: Array<String>) {
    if (!config.loadConfig()) {
        println("incorrect config format!")
        js("process.exit(1)")
    }

    val handlers: MutableMap<String, AbstractHandler> = mutableMapOf()

    val routeExecutor = RouteExecutorToUsb()
    handlers.put("/rc/control", Control())
    handlers.put("/loadBin", LoadBin())
    handlers.put("/route", SetRoute(routeExecutor))
    handlers.put("/getLocation", GetLocation())
    handlers.put("/debug/memoty", Memory())

    net.server.start(handlers, carServerPort)
    val mcConditionMonitor = McConditionMonitor.instance
    mcConditionMonitor.start()

    MicroController.instance.start()

}