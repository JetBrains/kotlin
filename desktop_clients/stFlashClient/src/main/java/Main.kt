import com.google.protobuf.ByteString
import com.martiansoftware.jsap.FlaggedOption
import com.martiansoftware.jsap.JSAP
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import proto.Carkot
import java.io.FileInputStream
import java.io.IOException

/**
 * Created by user on 7/11/16.
 */

fun main(args: Array<String>) {
    val jsap: JSAP = JSAP();
    val opthost = FlaggedOption("host").setStringParser(JSAP.STRING_PARSER).setRequired(true).setShortFlag('h').setLongFlag("host")
    val optPort = FlaggedOption("port").setStringParser(JSAP.INTEGER_PARSER).setDefault("8888").setRequired(false).setShortFlag('p').setLongFlag("port")
    val optBinPath = FlaggedOption("binFile").setStringParser(JSAP.STRING_PARSER).setRequired(true).setShortFlag('f').setLongFlag("binFile")
    val optmcuSystem = FlaggedOption("mcuSystem").setStringParser(JSAP.STRING_PARSER).setRequired(false).setDefault("0x08000000").setShortFlag('s')
    val optHelp = FlaggedOption("help").setStringParser(JSAP.BOOLEAN_PARSER).setRequired(false).setDefault("false").setLongFlag("help")
    jsap.registerParameter(opthost)
    jsap.registerParameter(optPort)
    jsap.registerParameter(optBinPath)
    jsap.registerParameter(optmcuSystem)
    jsap.registerParameter(optHelp)

    val config = jsap.parse(args)
    if (!config.success() || config.getBoolean("help")) {
        println(jsap.getHelp())
        return
    }

    val host = config.getString("host")
    val port = config.getInt("port")
    val base = config.getString("mcuSystem")
    val pathToFile = config.getString("binFile")

    var fileBytes: ByteArray = ByteArray(0);
    try {
        FileInputStream(pathToFile).use { fis ->
            fileBytes = ByteArray(fis.available())
            fis.read(fileBytes)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        println("error in read file $pathToFile")
        return;
    }

    val bytesBinTest = Carkot.Upload.newBuilder().setData(ByteString.copyFrom(fileBytes)).setBase(base).build().toByteArray()
    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/loadBin", Unpooled.copiedBuffer(bytesBinTest));
    request.headers().set(HttpHeaderNames.HOST, host)
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

    client.Client.sendRequest(request, host, port)
    println(client.ClientHandler.requestResult.code)
}
