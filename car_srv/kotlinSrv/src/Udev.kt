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
        monitor.on("add", { device ->
            if (isOurMcDevice(device)) {
                connectDevice(device)
            }
        })

        monitor.on("remove", { device ->
            if (isOurMcDevice(device)) {
                disconnectDevice()
            }
        })

        readFileIfMcConnected()
    }

    fun isOurMcDevice(device: dynamic): Boolean {
        val microController = MicroController.instance
        return (device.ID_VENDOR_ID == microController.vendorID)
                && (device.ID_MODEL_ID == microController.modelID)
                && (device.SUBSYSTEM == "tty")
    }

    fun disconnectDevice() {
        println("mc disconnected")
        MicroController.instance.transportFilePath = ""
        mcTransport.closeStreams()
    }

    fun connectDevice(device: dynamic) {
        println("mc connected. transport file is " + device.DEVNAME)
        MicroController.instance.transportFilePath = device.DEVNAME;
        mcTransport.initStreams(device.DEVNAME)
        exec("stty -F ${MicroController.instance.transportFilePath} raw -echo -echoe -echok")
    }

    fun readFileIfMcConnected() {
        val allTtyDevices = udev.list("tty")
        for (device in allTtyDevices) {
            if (isOurMcDevice(device)) {
                connectDevice(device)
                break
            }
        }
    }
}