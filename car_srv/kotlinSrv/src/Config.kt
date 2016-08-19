class Config(val configFileName: String = "config.cfg") {


    private var serverIp = "127.0.0.1"
    private var thisCarIp = "127.0.0.1"

    fun loadConfig(): Boolean {

        try {
            fs.accessSync(configFileName, fs.F_OK)
        } catch (e: dynamic) {
            // create it
            fs.openSync(configFileName, "w")
        }

        val data: String = fs.readFileSync(configFileName, "utf8")
        println("reader $data")
        data.split("\n").forEach { line ->
            val keyValue = line.split(":")
            if (!line.equals("")) {
                if (keyValue.size != 2) {
                    return false
                }

                println(keyValue.toString())
                when (keyValue[0]) {
                    "mainServerIp" -> serverIp = keyValue[1]
                    "thisServerIp" -> thisCarIp = keyValue[1]
                }
            }
        }
        return true
    }

    fun getIp(): String {
        return serverIp
    }

    fun getCarIp(): String {
        return thisCarIp
    }
}