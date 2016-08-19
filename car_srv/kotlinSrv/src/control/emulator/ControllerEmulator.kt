package control.emulator

import RouteRequest
import control.Controller

class ControllerEmulator : Controller {

    private val MOVE_VELOCITY = 0.3278
    private val ROTATION_VELOCITY = 12.3

    enum class MoveDirection {
        LEFT,
        RIGHT,
        FORWARD,
        BACKWARD,
        ERROR
    }

    override fun executeRoute(route: RouteRequest) {
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

        executeCommand(commands, 0)
    }

    override fun getSensorData(degrees: IntArray): IntArray {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun executeCommand(commands: List<Pair<MoveDirection, Int>>, currentCommandIdx: Int) {
//        if (currentCommandIdx == commands.size) {
//            MicroController.instance.car.routeDone()
//        }
//        val currentCommand = commands.get(currentCommandIdx)
//        MicroController.instance.car.move(currentCommand.first, currentCommand.second, {
//            executeCommand(commands, currentCommandIdx + 1)
//        })
    }

//    var moveDirection: MoveDirection = MoveDirection.STOP
//
//    fun stopCar() {
//        move(MoveDirection.STOP, 0, {})
//    }
//
//    fun refreshLocation(delta: Int) {
//        val deltaSeconds = delta.toDouble() / 1000
//        when (moveDirection) {
//            MoveDirection.FORWARD -> {
//                this.x += MOVE_VELOCITY * deltaSeconds * Math.cos(this.angle * Math.PI / 180);
//                this.y += MOVE_VELOCITY * deltaSeconds * Math.sin(this.angle * Math.PI / 180);
//            }
//            MoveDirection.BACKWARD -> {
//                this.x -= MOVE_VELOCITY * deltaSeconds * Math.cos(this.angle * Math.PI / 180);
//                this.y -= MOVE_VELOCITY * deltaSeconds * Math.sin(this.angle * Math.PI / 180);
//            }
//            MoveDirection.LEFT -> this.angle += ROTATION_VELOCITY * deltaSeconds
//            MoveDirection.RIGHT -> this.angle -= ROTATION_VELOCITY * deltaSeconds
//            else -> {
//
//            }
//        }
////        println("x=$x; y=$y; angle=$angle")
//    }
//
//    fun routeDone() {
//        controller.stopCar()
//    }
//
//    fun move(moveDirection: MoveDirection, value: Int, callBack: () -> Unit) {
//        //value - angle for rotation command and distance for forward/backward command
//        this.moveDirection = moveDirection
//        when (moveDirection) {
//            MoveDirection.STOP -> controller.stopCar()
//            MoveDirection.FORWARD -> controller.moveCarForward()
//            MoveDirection.BACKWARD -> controller.moveCarBackward()
//            MoveDirection.LEFT -> controller.moveCarLeft()
//            MoveDirection.RIGHT -> controller.moveCarRight()
//        }
//        if (moveDirection != MoveDirection.STOP) {
//            if (moveDirection == MoveDirection.FORWARD || moveDirection == MoveDirection.BACKWARD) {
//                controller.delay(getTimeForMoving(value, MOVE_VELOCITY), callBack)
//            } else {
//                controller.delay(getTimeForMoving(value, ROTATION_VELOCITY), callBack)
//            }
//        }
//    }
//
//    fun getTimeForMoving(value: Int, velocity: Double): Int {
//        return (1000 * Math.abs(value.toDouble()) / velocity).toInt()
//    }

}