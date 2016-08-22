import control.car.ControllerToUsb
import control.emulator.ControllerEmulator
import net.Client
import net.server.handlers.AbstractHandler
import net.server.handlers.debug.Memory
import net.server.handlers.flash.LoadBin
import net.server.handlers.main.GetLocation
import net.server.handlers.main.GetSonarData
import net.server.handlers.main.SetRoute
import net.server.handlers.rc.Control

val carServerPort: Int = 8888
val mainServerPort = 7925

val config = Config()
val fs: dynamic = require("fs")
fun main(args: Array<String>) {
    if (!config.loadConfig()) {
        println("incorrect config format!")
        js("process.exit(1)")
    }

    val handlers: MutableMap<String, AbstractHandler> = mutableMapOf()

    val carController = ControllerEmulator()

    handlers.put("/rc/control", Control())
    handlers.put("/loadBin", LoadBin())
    handlers.put("/sonar", GetSonarData(carController))
    handlers.put("/route", SetRoute(carController))
    handlers.put("/getLocation", GetLocation())
    handlers.put("/debug/memory", Memory())

    Client.instance.connectToServer(config.getCarIp(), carServerPort)

    mcTransport.setCallBack { bytes ->
        println("read: " + bytes.toString())
    }

    net.server.start(handlers, carServerPort)
    val mcConditionMonitor = McConditionMonitor.instance
    mcConditionMonitor.start()
}