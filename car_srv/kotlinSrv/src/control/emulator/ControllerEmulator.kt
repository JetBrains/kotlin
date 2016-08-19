package control.emulator

import CarState
import RouteRequest
import RouteResponse
import control.Controller
import encodeProtoBuf
import setTimeout
import kotlin.Pair

class ControllerEmulator : Controller {

    private val MOVE_VELOCITY = 32.78//sm/s
    private val ROTATION_VELOCITY = 12.3//degrees/s

    enum class MoveDirection {
        LEFT,
        RIGHT,
        FORWARD,
        BACKWARD,
        ERROR
    }

    override fun executeRoute(route: RouteRequest, callBack: (ByteArray) -> Unit) {
        val moveTimes = route.times
        val moveDirections = route.directions
        //list of move direction and time to this move in ms
        val commands: MutableList<Pair<MoveDirection, Int>> = mutableListOf()

        moveTimes.forEachIndexed { idx, value ->
            val moveDirection =
                    when (moveDirections[idx]) {
                        0 -> MoveDirection.FORWARD
                        1 -> MoveDirection.BACKWARD
                        2 -> MoveDirection.LEFT
                        3 -> MoveDirection.RIGHT
                        else -> MoveDirection.ERROR
                    }

            commands.add(Pair(moveDirection, value))
        }
        executeCommand(commands, 0, callBack)
    }

    override fun executeRequestSensorData(degrees: IntArray, callBack: (ByteArray) -> Unit) {

//        ByteArray

        //calculate distance
    }

    fun executeCommand(commands: List<Pair<MoveDirection, Int>>, currentCommandIdx: Int, callBack: (ByteArray) -> Unit) {
        if (currentCommandIdx == commands.size) {
            val responseMessage = RouteResponse.BuilderRouteResponse(0).build()
            callBack.invoke(encodeProtoBuf(responseMessage))
        }
        val currentCommand = commands.get(currentCommandIdx)

        //refresh car state
        val carInstance = CarState.instance
        val commandTime = currentCommand.second
        when (currentCommand.first) {
            MoveDirection.FORWARD -> carInstance.moving((commandTime * MOVE_VELOCITY).toInt() / 1000)
            MoveDirection.BACKWARD -> carInstance.moving(-(commandTime * MOVE_VELOCITY).toInt() / 1000)
            MoveDirection.RIGHT -> carInstance.rotate((commandTime * ROTATION_VELOCITY).toInt() / 1000)
            MoveDirection.LEFT -> carInstance.rotate(-(commandTime * ROTATION_VELOCITY).toInt() / 1000)
            else -> {
            }
        }

        setTimeout({
            executeCommand(commands, currentCommandIdx + 1, callBack)
        }, currentCommand.second)
    }



}