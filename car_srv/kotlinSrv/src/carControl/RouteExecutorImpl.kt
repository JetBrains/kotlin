package carControl

import RouteRequest

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
        if (currentCommandIdx == commands.size) {
            MicroController.instance.car.routeDone()
        }
        val currentCommand = commands.get(currentCommandIdx)
        MicroController.instance.car.move(currentCommand.first, currentCommand.second, {
            executeCommand(commands, currentCommandIdx + 1)
        })
    }

}