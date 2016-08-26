package control.emulator

import CarState
import RouteMetricRequest
import RouteRequest
import RouteResponse
import SonarRequest
import SonarResponse
import control.Controller
import encodeProtoBuf
import geometry.Line
import geometry.Vector
import room.Room
import kotlin.Pair

class ControllerEmulator : Controller {

    private val MOVE_VELOCITY = 0.05//sm/ms
    private val ROTATION_VELOCITY = 0.05//degrees/ms

    private val ADD_RANDOM = false

    enum class MoveDirection {
        LEFT,
        RIGHT,
        FORWARD,
        BACKWARD,
        ERROR
    }

    override fun executeRoute(route: RouteRequest, callback: (ByteArray) -> Unit) {
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
        executeCommand(commands, 0, callback)
    }

    override fun executeMetricRoute(request: RouteMetricRequest, callback: (ByteArray) -> Unit) {
        throw UnsupportedOperationException()
    }

    override fun executeRequestSensorData(sonarRequest: SonarRequest, callback: (ByteArray) -> Unit) {
        val angles = sonarRequest.angles
        val xSensor0 = CarState.instance.x
        val ySensor0 = CarState.instance.y
        val carAngle = CarState.instance.angle

        val distances = arrayListOf<Int>()
        angles.forEach { angle ->
            val angleFinal = getSensorAngle(angle, carAngle)
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
                in (90..270) -> {
                    xSensor1 = xSensor0 - 1
                    ySensor1 = ySensor0 + (xSensor1 - xSensor0) * Math.tan(angleFinal * Math.PI / 180)
                }
                else -> {
                    xSensor1 = xSensor0 + 1
                    ySensor1 = ySensor0 + (xSensor1 - xSensor0) * Math.tan(angleFinal * Math.PI / 180)
                }
            }
            val sensorLine = Line(ySensor0 - ySensor1, xSensor1.toDouble() - xSensor0,
                    xSensor0 * ySensor1 - ySensor0 * xSensor1)

            val sensorVector = Vector(xSensor0.toDouble(), ySensor0.toDouble(), xSensor1.toDouble(), ySensor1)
            val distance = getDistance(xSensor0, ySensor0, sensorLine, sensorVector)
            if (distance == -1) {
                distances.add(distance)
            } else {
                val delta = if (ADD_RANDOM) getRandomIntFrom(IntArray(5, { x -> x - 2 })) else 0//return one of -2 -1 0 1 or 2
                distances.add(distance + delta)
            }
        }
        val responseMessage = SonarResponse.BuilderSonarResponse(distances.toIntArray()).build()
        val bytesMessage = encodeProtoBuf(responseMessage)
        callback.invoke(bytesMessage)
    }

    private fun getDistance(xSensor0: Int, ySensor0: Int, sensorLine: Line, sensorVector: Vector): Int {
        var result = Int.MAX_VALUE
        for (wall in Room.walls) {
            val wallLine = wall.line
            val slope = sensorLine.A * wallLine.B - sensorLine.B * wallLine.A
            if (Math.abs(slope) < 0.01) {
                //line is parallel.
                continue
            }
            val xIntersection = (sensorLine.B * wallLine.C - wallLine.B * sensorLine.C) / slope
            val yIntersection = (sensorLine.C * wallLine.A - wallLine.C * sensorLine.A) / slope

            //filters by direction and intersection position
            val intersectionVector = Vector(xSensor0.toDouble(), ySensor0.toDouble(), xIntersection, yIntersection)
            if (intersectionVector.scalarProduct(sensorVector) < 0) {
                continue
            }
            val wallVector1 = Vector(xIntersection, yIntersection, wall.xTo.toDouble(), wall.yTo.toDouble())
            val wallVector2 = Vector(xIntersection, yIntersection, wall.xFrom.toDouble(), wall.yFrom.toDouble())
            if (wallVector1.scalarProduct(wallVector2) > 0) {
                continue
            }
            val currentDistance = Math.round(Math.sqrt(Math.pow(xIntersection - xSensor0, 2.0)
                    + Math.pow(yIntersection - ySensor0, 2.0)))
            if (currentDistance < result) {
                result = currentDistance
            }
        }
//        if (result < 5 || result > 500) {
//            return -1
//        }
        return result
    }

    //return sensor angle in [0, 360) with considering car rotation
    private fun getSensorAngle(requestAngle: Int, carAngle: Int): Int {
        var angleTmp = carAngle - requestAngle
        while (angleTmp < 0) {
            angleTmp += 360
        }
        return angleTmp % 360
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
            return
        }
        val currentCommand = commands[currentCommandIdx]

        //refresh car state
        val carInstance = CarState.instance
        val commandTime = currentCommand.second
        val delta = if (ADD_RANDOM) Math.random() * 0.2 + 0.9 else 1.0// delta in [0.9, 1.1)
        val commandTimeIncludeRandom = (commandTime * delta).toInt()
        when (currentCommand.first) {
            MoveDirection.FORWARD -> carInstance.moving((commandTimeIncludeRandom * MOVE_VELOCITY).toInt())
            MoveDirection.BACKWARD -> carInstance.moving(-(commandTimeIncludeRandom * MOVE_VELOCITY).toInt())
            MoveDirection.RIGHT -> carInstance.rotate(-(commandTimeIncludeRandom * ROTATION_VELOCITY).toInt())
            MoveDirection.LEFT -> carInstance.rotate((commandTimeIncludeRandom * ROTATION_VELOCITY).toInt())
            else -> {
            }
        }

//        setTimeout({
        executeCommand(commands, currentCommandIdx + 1, callBack)
//        }, currentCommand.second)
    }


}