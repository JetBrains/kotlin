import com.google.protobuf.ByteString
import com.martiansoftware.jsap.FlaggedOption
import com.martiansoftware.jsap.JSAP
import com.martiansoftware.jsap.JSAPResult
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import proto.Carkot
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter
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
    saveFileConfig(actualValues)
    var fileBytes: ByteArray = ByteArray(0);
    try {
        FileInputStream(flashFilePath).use { fis ->
            fileBytes = ByteArray(fis.available())
            fis.read(fileBytes)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        println("error in read file $flashFilePath")
        return;
    }

    val bytesBinTest = Carkot.Upload.newBuilder().setData(ByteString.copyFrom(fileBytes)).setBase(mcuSystem).build().toByteArray()
    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/loadBin", Unpooled.copiedBuffer(bytesBinTest));
    request.headers().set(HttpHeaderNames.HOST, host)
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

    client.Client.sendRequest(request, host, port)
    println(client.ClientHandler.requestResult.code)
    println(client.ClientHandler.requestResult.stdErr)
    println(client.ClientHandler.requestResult.stdOut)
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