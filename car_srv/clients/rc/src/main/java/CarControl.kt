import clientClasses.Client
import java.io.ByteArrayOutputStream

/**
 * Created by user on 7/14/16.
 */
class CarControl constructor(client: Client) {

    val client: Client
    var sid: Int

    init {
        this.client = client
        this.sid = 0
    }

    fun executeCommand(command: DirectionRequest.Command) {

        val directionBuilder = DirectionRequest.BuilderDirectionRequest()
        directionBuilder.setCommand(command).setSid(sid)
        val byteArrayStream = ByteArrayOutputStream()

        directionBuilder.build().writeTo(CodedOutputStream(byteArrayStream))
        val request = getDefaultHttpRequest(client.host, controlUrl, byteArrayStream)

        client.sendRequest(request)
    }
}


