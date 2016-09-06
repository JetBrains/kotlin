import control.car.ControllerToUsb
import control.emulator.ControllerEmulator
import control.emulator.Rng
import net.Client
import net.server.handlers.AbstractHandler
import net.server.handlers.debug.Memory
import net.server.handlers.debug.Sonar
import net.server.handlers.flash.LoadBin
import net.server.handlers.main.*
import net.server.handlers.rc.Control
import room.Room

val carServerPort: Int = 8888
val mainServerPort = 7925

val config = Config()
val fs: dynamic = require("fs")
fun main(args: Array<String>) {

    if (!config.loadConfig()) {
        println("incorrect config format!")
        js("process.exit(1)")
    }
    val clArgs = js("process.argv")
    var runAsEmulator = false
    var runTests = false
    var nextSeed = false
    var room: Room = Room.testRoom1()
    for (arg in clArgs) {
        if (nextSeed) {
            Rng.SEED = parseInt(arg, 10).toLong()
            Rng.curState = Rng.SEED.toLong()
            nextSeed = false
        }
        when (arg) {
            "-t" -> runTests = true
            "-e" -> runAsEmulator = true
            "-tr1" -> room = Room.testRoom1()
            "-tr2" -> room = Room.testRoom2()
            "-tr3" -> room = Room.testRoom3()
            "-r" -> Rng.ADD_RANDOM = true
            "-s" -> nextSeed = true
        }
        if (arg.toString().contains("--room=")) {
            val pathToFile = arg.toString().split("=")[1].replace("\"", "")
            val data: String = fs.readFileSync(pathToFile, "utf8")
            val roomOrNull = Room.roomFromString(data)
            if (roomOrNull == null) {
                println("error parsing string $data \nto room")
                return
            }
            room = roomOrNull
        }
    }
    val carController =
            if (runAsEmulator) {
                McState.instance.connect("")
                ControllerEmulator(room)
            } else {
                ControllerToUsb()
            }

    if (runTests) {
        runTests()
    }

    val handlers = mutableMapOf<String, AbstractHandler>()
    handlers.put("/rc/control", Control())
    handlers.put("/loadBin", LoadBin())
    handlers.put("/sonar", GetSonarData(carController))
    handlers.put("/sonarExplore", SonarExplore(carController))
    handlers.put("/route", SetRoute(carController))
    handlers.put("/routeMetric", SetRouteMetric(carController))
    handlers.put("/getLocation", GetLocation())
    handlers.put("/debug/memory", Memory())
    handlers.put("/debug/sonar", Sonar())

    Client.instance.connectToServer(config.getCarIp(), carServerPort)

    mcTransport.setCallBack { bytes ->
        println("read: " + bytes.toString())
    }

    net.server.start(handlers, carServerPort)
    val mcConditionMonitor = McConditionMonitor.instance
    mcConditionMonitor.start()
}