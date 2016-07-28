import DirectionRequest.Command
import clientClasses.Client
import clientClasses.ClientHandler
import com.martiansoftware.jsap.FlaggedOption
import com.martiansoftware.jsap.JSAP
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

/**
 * Created by user on 7/14/16.
 */

//available direction symbols and command for symbol
val correctDirectionMap = mapOf<Char, Command>(
        Pair('w', Command.forward),
        Pair('s', Command.backward),
        Pair('a', Command.left),
        Pair('d', Command.right),
        Pair('h', Command.stop))

//urls
val heartbeatUrl = "/rc/heartbeat"
val connectUrl = "/rc/connect"
val disconnectUrl = "/rc/disconnect"
val controlUrl = "/rc/control"


fun main(args: Array<String>) {
    val jsap: JSAP = JSAP()
    setOptions(jsap)
    val config = jsap.parse(args)
    if (!config.success() || config.getBoolean("help")) {
        println(jsap.getHelp())
        return
    }
    val host = config.getString("host")
    val port = config.getInt("port")

    val ipRegex = Regex("^(?:[0-9]{1,3}.){3}[0-9]{1,3}$")
    if (!ipRegex.matches(host)) {
        println("incorrect server address.")
        return
    }

    val client = Client(host, port)
    val carControl = CarControl(client)

    //connect and get SID
    var byteArrayStream = ByteArrayOutputStream()
    byteArrayStream.use {
        val rcSessionUpRequest = getDefaultHttpRequest(client.host, connectUrl, byteArrayStream)
        client.sendRequest(rcSessionUpRequest)
    }
    val sid = synchronized(ClientHandler.requestResult, {
        if (ClientHandler.requestResult.code != 0) {
            println("connection refused")
            printRequestResult()
            0
        } else {
            ClientHandler.requestResult.sid
        }
    })
    if (sid == 0) {
        client.close()
        return
    }

    //init heartbeat
    val byteArrayOutputStream = ByteArrayOutputStream()
    byteArrayOutputStream.use {
        HeartBeatRequest.BuilderHeartBeatRequest().setSid(sid).build().writeTo(CodedOutputStream(byteArrayOutputStream))
    }
    val heartbeatThread = thread {
        while (true) {
            val heartbeatRequest = getDefaultHttpRequest(client.host, heartbeatUrl, byteArrayOutputStream)
            client.sendRequest(heartbeatRequest)
            try {
                Thread.sleep(400)
            } catch (e: InterruptedException) {
                break
            }
            printRequestResult()
        }
    }

    carControl.sid = sid
    initTextInterface(carControl)

    //disconnect
    byteArrayStream = ByteArrayOutputStream()
    byteArrayStream.use {
        SessionDownRequest.BuilderSessionDownRequest().setSid(sid).build().writeTo(CodedOutputStream(byteArrayStream))
    }
    val rcSessionDownRequest = getDefaultHttpRequest(client.host, disconnectUrl, byteArrayStream)
    client.sendRequest(rcSessionDownRequest)
    heartbeatThread.interrupt()
    client.close()
    printRequestResult()
}

fun getDefaultHttpRequest(host: String, url: String, byteArrayStream: ByteArrayOutputStream): DefaultFullHttpRequest {
    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, url, Unpooled.copiedBuffer(byteArrayStream.toByteArray()));
    request.headers().set(HttpHeaderNames.HOST, host)
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
    return request
}

fun initTextInterface(carControl: CarControl) {
    val helpMessage = "type w, s, a, d, h for command forward, backward, left, right, stop. to exit type q or quit ";
    println(helpMessage)

    while (true) {
        val nextLine = readLine()
        if (nextLine == null) {
            return
        }
        if (nextLine.equals("q", true) || nextLine.equals("quit", true)) {
            return
        } else if (nextLine.length != 1) {
            println("incorrect argument \"$nextLine\"")
            println(helpMessage)
        } else {
            val directionChar = nextLine.get(0)
            if (!correctDirectionMap.containsKey(directionChar)) {
                println("incorrect argument \"$nextLine\"")
                println(helpMessage)
            } else {
                carControl.executeCommand(correctDirectionMap.get(directionChar)!!)
                printRequestResult()
            }
        }
    }
}

fun printRequestResult() {
    synchronized(ClientHandler.requestResult, {
        if (ClientHandler.requestResult.code != 0) {
            println("result code: ${ClientHandler.requestResult.code}\n" +
                    "error message: ${ClientHandler.requestResult.errorString}\n" +
                    "sid: ${ClientHandler.requestResult.sid}")
        }
    })
}

fun setOptions(jsap: JSAP) {
    val opthost = FlaggedOption("host").setStringParser(JSAP.STRING_PARSER).setRequired(true).setShortFlag('h').setLongFlag("host")
    opthost.setHelp("write here only ip address. domain name (e.g. vk.com is incorrect)")
    val optPort = FlaggedOption("port").setStringParser(JSAP.INTEGER_PARSER).setDefault("8888").setRequired(false).setShortFlag('p').setLongFlag("port")
    val optHelp = FlaggedOption("help").setStringParser(JSAP.BOOLEAN_PARSER).setRequired(false).setDefault("false").setLongFlag("help")
    jsap.registerParameter(opthost)
    jsap.registerParameter(optPort)
    jsap.registerParameter(optHelp)
}
