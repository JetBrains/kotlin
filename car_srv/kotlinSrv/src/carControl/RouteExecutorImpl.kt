package carControl


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

    override fun executeRoute(route: dynamic) {
        val wayPoints = route.way_points
        val commands: MutableList<Pair<MoveDirection, Double>> = mutableListOf()
        for (wayPoint in wayPoints) {
            val angle: Double = wayPoint.angle_delta
            val distance: Double = wayPoint.distance
            if (angle != 0.0) {
                val command = if (angle > 180) {
                    MoveDirection.RIGHT
                } else {
                    MoveDirection.LEFT
                }
                commands.add(Pair(command, angle))
            }
            if (distance != 0.0) {
                val command = if (distance > 0) {
                    MoveDirection.FORWARD
                } else {
                    MoveDirection.BACKWARD
                }
                commands.add(Pair(command, distance))
            }
        }
        commands.add(Pair(MoveDirection.STOP, 0.0))
        executeCommand(commands, 0)
    }

    fun executeCommand(commands: List<Pair<MoveDirection, Double>>, currentCommandIdx: Int) {
        if (currentCommandIdx == commands.size) {
            MicroController.instance.car.routeDone()
        }
        val currentCommand = commands.get(currentCommandIdx)
        MicroController.instance.car.move(currentCommand.first, currentCommand.second, {
            executeCommand(commands, currentCommandIdx + 1)
        })
    }

}