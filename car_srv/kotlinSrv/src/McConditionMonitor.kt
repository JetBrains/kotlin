/**
 * Created by user on 7/27/16.
 * observable class, check mc condition and notify observers about changes
 */
class McConditionMonitor private constructor() : MCConnectObservable<String> {

    private val udev: dynamic = require("udev")
    private val exec: dynamic = require("child_process").execSync
    private val observersList: MutableList<MCConnectObserver<String>> = arrayListOf()

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

    override fun addObserver(MCConnectObserver: MCConnectObserver<String>) {
        observersList.add(MCConnectObserver)
    }

    override fun removeObserver(MCConnectObserver: MCConnectObserver<String>) {
        observersList.remove(MCConnectObserver)
    }

    private fun notifyMCConnect(nodeName: String) {
        for (observer in observersList) {
            observer.connect(nodeName)
        }
    }

    private fun notifyMCDisconnect() {
        for (observer in observersList) {
            observer.disconnect()
        }
    }

    private fun isOurMcDevice(device: dynamic): Boolean {
        val microController = MicroController.instance
        return (device.ID_VENDOR_ID == microController.vendorID)
                && (device.ID_MODEL_ID == microController.modelID)
                && (device.SUBSYSTEM == "tty")
    }

    private fun disconnectDevice() {
        println("mc disconnected")
        notifyMCDisconnect()
    }

    private fun connectDevice(device: dynamic) {
        val transportFile: String = device.DEVNAME
        mcTransport.initStreams(transportFile)
        McState.instance.connect()
        println("mc connected. transport file is " + transportFile)
        exec("stty -F $transportFile raw -echo -echoe -echok")
        notifyMCConnect(transportFile)
    }

    private fun readFileIfMcConnected() {
        val allTtyDevices = udev.list("tty")
        for (device in allTtyDevices) {
            if (isOurMcDevice(device)) {
                connectDevice(device)
                break
            }
        }
    }

    companion object {
        val instance = McConditionMonitor()
    }

}