import client.Client
import com.martiansoftware.jsap.FlaggedOption
import com.martiansoftware.jsap.JSAP

/**
 * Created by user on 7/14/16.
 */

val correctDirectionValues: Array<Char> = arrayOf('f', 'b', 'l', 'r', 's');

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
    val direction = config.getChar("direction")

    val client = Client(host, port)
    val carControl = CarControl(client)
    if (direction.equals('t', true)) {
        initTextInterface(carControl)
    } else if (correctDirectionValues.contains(direction)) {
        carControl.executeCommand(direction)
    } else {
        println("incorrect direction.")
        println(jsap.getHelp())
    }
}

fun initTextInterface(carControl: CarControl) {
    val helpMessage = "type f, b, l, r, s for command forward, backward, left, right, stop. to exit type q or quit ";
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
            if (!correctDirectionValues.contains(directionChar)) {
                println("incorrect argument \"$nextLine\"")
                println(helpMessage)
            } else {
                carControl.executeCommand(directionChar)
            }
        }
    }
}

fun setOptions(jsap: JSAP) {
    val opthost = FlaggedOption("host").setStringParser(JSAP.STRING_PARSER).setRequired(true).setShortFlag('h').setLongFlag("host")
    val optPort = FlaggedOption("port").setStringParser(JSAP.INTEGER_PARSER).setDefault("8888").setRequired(false).setShortFlag('p').setLongFlag("port")
    val optHelp = FlaggedOption("help").setStringParser(JSAP.BOOLEAN_PARSER).setRequired(false).setDefault("false").setLongFlag("help")
    val optDirection = FlaggedOption("direction").setStringParser(JSAP.CHARACTER_PARSER).setRequired(false).setDefault("t").setShortFlag('d')
    optDirection.setHelp("move direction: available values - one symbol:\n\"f (forward),b (backward),l (left),r (right),s (stop)\". example: -d f\nwithout argument used default \"t\" - running text interface")
    jsap.registerParameter(opthost)
    jsap.registerParameter(optPort)
    jsap.registerParameter(optHelp)
    jsap.registerParameter(optDirection)
}
