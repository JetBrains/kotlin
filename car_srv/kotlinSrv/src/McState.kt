class McState : MCConnectObserver<String> {

    val VENDORID = "0483"
    val MODELID = "5740"

    private var connected = false
    private var transportFileName = ""

    init {
        McConditionMonitor.instance.addObserver(this)
    }

    fun isConnected(): Boolean {
        return connected
    }

    override fun connect(transportFileName: String) {
        this.transportFileName = transportFileName
        connected = true
    }

    override fun disconnect() {
        connected = false
    }

    companion object {
        val instance = McState()
    }

}