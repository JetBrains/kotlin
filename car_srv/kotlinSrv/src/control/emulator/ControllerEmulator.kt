package control.emulator

import CarState
import RouteRequest
import RouteResponse
import SonarResponse
import control.Controller
import encodeProtoBuf
import room.Room
import room.Line
import setTimeout
import kotlin.Pair
import SonarRequest

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

    override fun executeRequestSensorData(sonarRequest: SonarRequest, callBack: (ByteArray) -> Unit) {

        val angles = sonarRequest.angles
        val xSensor0 = CarState.instance.x
        val ySensor0 = CarState.instance.y
        val carAngle = CarState.instance.angle

        val distances = arrayListOf<Int>()
        angles.forEach { angle ->
            //need angle in [0,360)
            var angleTmp = carAngle - angle
            while (angleTmp < 0) {
                angleTmp += 360
            }
            val angleFinal = angleTmp % 360
            val xSensor1: Int
            val ySensor1: Double
            //tg can be equal to inf if angle = 90 or 270. it vertical line. x1 = x0, y1 = y0+-[any number. eg 1]
            when (angleFinal) {
                90 -> {
                    xSensor1 = xSensor0
                    ySensor1 = (ySensor0 + 1).toDouble()
                }
                270 -> {
                    xSensor1 = xSensor0
                    ySensor1 = (ySensor0 - 1).toDouble()
                }
                else -> {
                    xSensor1 = xSensor0 + 1
                    ySensor1 = ySensor0 + (xSensor1 - xSensor0) * Math.tan(angleFinal * Math.PI / 180)
                }
            }
            val sensorLine = Line(ySensor0 - ySensor1, xSensor1.toDouble() - xSensor0, xSensor0 * ySensor1 - ySensor0 * xSensor1)
            val sensorVectorX = xSensor1 - xSensor0
            val sensorVectorY = ySensor1 - ySensor0
            var distance = Int.MAX_VALUE
            for (wall in Room.walls) {
                val wallLine = wall.line
                val coef = sensorLine.A * wallLine.B - sensorLine.B * wallLine.A
                if (Math.abs(coef) < 0.01) {
                    //line is parallel.
                    continue
                }
                val xIntersection = (sensorLine.B * wallLine.C - wallLine.B * sensorLine.C) / coef
                val yIntersection = (sensorLine.C * wallLine.A - wallLine.C * sensorLine.A) / coef

                //filters by direction and intersection position
                val IntersectionVectorX = xIntersection - xSensor0
                val IntersectionVectorY = yIntersection - ySensor0
                if (IntersectionVectorX * sensorVectorX + IntersectionVectorY * sensorVectorY < 0) {
                    continue
                }
                val wallVectorXTo = wall.xTo - xIntersection
                val wallVectorYTo = wall.yTo - yIntersection
                val wallVectorXFrom = wall.xFrom - xIntersection
                val wallVectorYFrom = wall.yFrom - yIntersection
                if (wallVectorXTo * wallVectorXFrom + wallVectorYTo * wallVectorYFrom > 0) {
                    continue
                }
                val currentDistance = Math.round(Math.sqrt(Math.pow(xIntersection - xSensor0, 2.0)
                        + Math.pow(yIntersection - ySensor0, 2.0)))
                if (currentDistance < distance) {
                    distance = currentDistance
                }
            }
            println("dist $distance")
            if (distance < 5 || distance > 500) {
                distances.add(-1)
            } else {
                val delta = getRandomIntFrom(IntArray(5, { x -> x - 2 }))//return one of -2 -1 0 1 or 2
                distances.add(distance + delta)
            }
        }
        val responseMessage = SonarResponse.BuilderSonarResponse(distances.toIntArray()).build()
        val bytesMessage = encodeProtoBuf(responseMessage)
        callBack.invoke(bytesMessage)
    }

    //return random int from array
    private fun getRandomIntFrom(values: IntArray): Int {
        val randomValue = (Math.random() * 1000).toInt()//value in [0,999]
        val randomArrayIdx = randomValue * values.size / 1000//index in [0, values.size-1]
        return values[randomArrayIdx]
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
        val delta = Math.random() * 0.2 + 0.9// delta in [0.9, 1.1)
        val commandTimeIncludeRandom = (commandTime * delta).toInt()
        when (currentCommand.first) {
            MoveDirection.FORWARD -> carInstance.moving((commandTimeIncludeRandom * MOVE_VELOCITY).toInt() / 1000)
            MoveDirection.BACKWARD -> carInstance.moving(-(commandTimeIncludeRandom * MOVE_VELOCITY).toInt() / 1000)
            MoveDirection.RIGHT -> carInstance.rotate((commandTimeIncludeRandom * ROTATION_VELOCITY).toInt() / 1000)
            MoveDirection.LEFT -> carInstance.rotate(-(commandTimeIncludeRandom * ROTATION_VELOCITY).toInt() / 1000)
            else -> {
            }
        }

        setTimeout({
            executeCommand(commands, currentCommandIdx + 1, callBack)
        }, currentCommand.second)
    }


}