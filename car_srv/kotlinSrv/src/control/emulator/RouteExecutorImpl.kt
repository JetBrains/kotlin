package control.emulator

import RouteRequest
import control.RouteExecutor

/**
 * Created by user on 7/27/16.
 */
class RouteExecutorImpl : RouteExecutor {

    enum class MoveDirection {
        LEFT,
        RIGHT,
        FORWARD,
        BACKWARD,
        STOP
    }

    override fun executeRoute(route: RouteRequest) {
        val angles = route.angles
        val distances = route.distances
        val commands: MutableList<Pair<MoveDirection, Int>> = mutableListOf()
        for (i in 0..angles.size - 1) {
            val angle: Int = angles[i]
            val distance: Int = distances[i]
            if (angle != 0) {
                val command = if (angle > 180) {
                    MoveDirection.RIGHT
                } else {
                    MoveDirection.LEFT
                }
                commands.add(Pair(command, angle))
            }
            if (distance != 0) {
                val command = if (distance > 0) {
                    MoveDirection.FORWARD
                } else {
                    MoveDirection.BACKWARD
                }
                commands.add(Pair(command, distance))
            }
        }
        commands.add(Pair(MoveDirection.STOP, 0))
        executeCommand(commands, 0)
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