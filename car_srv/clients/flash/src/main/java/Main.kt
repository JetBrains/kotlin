import client.Client
import com.martiansoftware.jsap.FlaggedOption
import com.martiansoftware.jsap.JSAP
import com.martiansoftware.jsap.JSAPResult
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import java.io.*
import java.util.*

/**
 * Created by user on 7/11/16.
 */
val configFileName = "./config.cfg"

fun main(args: Array<String>) {
    val jsap: JSAP = JSAP()
    setOptions(jsap)

    val clArgsConfig = jsap.parse(args)
    if (!clArgsConfig.success() || clArgsConfig.getBoolean("help")) {
        println(jsap.getHelp())
        return
    }
    val fileConfig = readFileConfig()

    val host = getActualValue("host", clArgsConfig, fileConfig)
    val port = getActualValue("port", clArgsConfig, fileConfig, "8888").toInt()
    val mcuSystem = getActualValue("mcuSystem", clArgsConfig, fileConfig, "0x08000000")
    val flashFilePath = getActualValue("flash", clArgsConfig, fileConfig)

    val actualValues = mapOf<String, String>(Pair("host", host), Pair("port", port.toString()), Pair("mcuSystem", mcuSystem), Pair("flash", flashFilePath))

    if (!isCorrectArguments(actualValues)) {
        return
    }
    var fileBytes: ByteArray = ByteArray(0);
    try {
        FileInputStream(flashFilePath).use { fis ->
            fileBytes = ByteArray(fis.available())
            fis.read(fileBytes)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        println("error reading file $flashFilePath")
        return;
    }

    saveFileConfig(actualValues)

    val uploadObject = Upload.BuilderUpload().setData(fileBytes).setBase(mcuSystem).build()
    val uploadBytesStream = ByteArrayOutputStream()
    uploadObject.writeTo(CodedOutputStream(uploadBytesStream))
    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/loadBin", Unpooled.copiedBuffer(uploadBytesStream.toByteArray()));
    request.headers().set(HttpHeaderNames.HOST, host)
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

    val clientInstanse = Client(host, port)
    clientInstanse.sendRequest(request)
    val requestResult = client.ClientHandler.requestResult
    var printString: String = ""
    synchronized(requestResult, {
        printString = "result code: ${requestResult.code}\n" +
                "result error message: ${requestResult.stdErr}\n" +
                "result out message: ${requestResult.stdOut}"
    })
    println(printString)
}

fun isCorrectArguments(actualValues: Map<String, String>): Boolean {
    //host and flash file its required.
    val host = actualValues.getOrElse("host", { "" })
    val flashFilePath = actualValues.getOrElse("flash", { "" })

    if (host.equals("") || flashFilePath.equals("")) {
        println("incorrect args (host/flash file must be initialized)")
        return false;
    }
    val file = File(flashFilePath)
    if (!file.exists()) {
        println("file ${file.absoluteFile} not exist")
        return false
    }
    val ipRegex = Regex("^(?:[0-9]{1,3}.){3}[0-9]{1,3}$")//bad regex. skip ip with first zero (10.05.1.1) and with numbers more 255 (300.300.300.300). this ips is correct for this regex
    if (!ipRegex.matches(host)) {
        println("incorrect server address.")
        return false
    }
    return true
}

fun saveFileConfig(actualValues: Map<String, String>) {
    var dataToFile = ""
    actualValues.forEach { e -> dataToFile += ("${e.key}:${e.value}\n") }
    val file = File(configFileName)
    if (!file.exists()) {
        try {
            file.createNewFile()
        } catch (e: IOException) {
            println("error creating config file. parameters don't saved")
            e.printStackTrace()
            return
        }
    }
    PrintWriter(file).use { printWriter ->
        printWriter.write(dataToFile)
        printWriter.flush()
    }
}

fun getActualValue(argName: String, commandLineConfig: JSAPResult, fileConfig: Map<String, String>, defaultValue: String = ""): String {
    return if (commandLineConfig.getString(argName) != null) {
        commandLineConfig.getString(argName)
    } else {
        fileConfig.getOrElse(argName, {
            defaultValue
        })
    }
}

fun readFileConfig(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    try {
        Scanner(File(configFileName)).use { scanner ->
            while (scanner.hasNext()) {
                val currentString = scanner.nextLine()
                val keyValuePair = currentString.split(":")
                if (keyValuePair.size != 2) {
                    continue
                } else {
                    result.put(keyValuePair[0], keyValuePair[1])
                }
            }
        }
    } catch (e: IOException) {

    }
    return result;
}

fun setOptions(jsap: JSAP) {
    val opthost = FlaggedOption("host").setStringParser(JSAP.STRING_PARSER).setRequired(false).setShortFlag('h').setLongFlag("host")
    opthost.setHelp("write here only ip address. domain name (e.g. vk.com is incorrect)")
    val optPort = FlaggedOption("port").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setShortFlag('p').setLongFlag("port")
    val optBinPath = FlaggedOption("flash").setStringParser(JSAP.STRING_PARSER).setRequired(false).setShortFlag('f').setLongFlag("flash")
    val optmcuSystem = FlaggedOption("mcuSystem").setStringParser(JSAP.STRING_PARSER).setRequired(false).setShortFlag('s')
    val optHelp = FlaggedOption("help").setStringParser(JSAP.BOOLEAN_PARSER).setRequired(false).setDefault("false").setLongFlag("help")
    jsap.registerParameter(opthost)
    jsap.registerParameter(optPort)
    jsap.registerParameter(optBinPath)
    jsap.registerParameter(optmcuSystem)
    jsap.registerParameter(optHelp)
}