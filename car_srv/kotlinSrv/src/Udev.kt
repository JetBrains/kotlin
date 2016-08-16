/**
 * Created by user on 7/27/16.
 */
class Udev {

    val udev: dynamic
    val exec: dynamic

    init {
        udev = require("udev")
        exec = require("child_process").execSync
    }

    fun start() {
        val monitor = udev.monitor()
        val microController = MicroController.instance
        monitor.on("add", { device ->
            if (device.ID_VENDOR_ID == microController.vendorID && device.ID_MODEL_ID == microController.modelID && device.SUBSYSTEM == "tty") {
                //mc connected
                println("mc connected. transport file is " + device.DEVNAME)
                microController.transportFilePath = device.DEVNAME;
                mcTransport.initStreams(device.DEVNAME)
                exec("stty -F ${MicroController.instance.transportFilePath} raw -echo -echoe -echok")
            }
        })

        monitor.on("remove", { device ->
            if (device.ID_VENDOR_ID == microController.vendorID && device.ID_MODEL_ID == microController.modelID && device.SUBSYSTEM == "tty") {
                //mc disconnected
                println("mc disconnected")
                microController.transportFilePath = ""
                mcTransport.closeStreams()
            }
        })
    }
}