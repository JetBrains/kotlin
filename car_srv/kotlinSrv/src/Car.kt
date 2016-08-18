import carControl.Control
import carControl.RouteExecutor
import carControl.RouteExecutorImpl.MoveDirection

private val MOVE_VELOCITY = 0.3278
private val ROTATION_VELOCITY = 12.3

// TODO make Car class mutable state saving entity
// that almost doesn't have behavior
/**
 * Created by user on 7/27/16.
 */
class Car constructor(val routeExecutor: RouteExecutor, val controller: Control) {
    //position
    var x: Double
    var y: Double
    var angle: Double

    var moveDirection: MoveDirection = MoveDirection.STOP


    init {
        this.x = 0.0
        this.y = 0.0
        this.angle = 0.0
    }

    fun stopCar() {
        move(MoveDirection.STOP, 0, {})
    }

    fun refreshLocation(delta: Int) {
        val deltaSeconds = delta.toDouble() / 1000
        when (moveDirection) {
            MoveDirection.FORWARD -> {
                this.x += MOVE_VELOCITY * deltaSeconds * Math.cos(this.angle * Math.PI / 180);
                this.y += MOVE_VELOCITY * deltaSeconds * Math.sin(this.angle * Math.PI / 180);
            }
            MoveDirection.BACKWARD -> {
                this.x -= MOVE_VELOCITY * deltaSeconds * Math.cos(this.angle * Math.PI / 180);
                this.y -= MOVE_VELOCITY * deltaSeconds * Math.sin(this.angle * Math.PI / 180);
            }
            MoveDirection.LEFT -> this.angle += ROTATION_VELOCITY * deltaSeconds
            MoveDirection.RIGHT -> this.angle -= ROTATION_VELOCITY * deltaSeconds
            else -> {

            }
        }
//        println("x=$x; y=$y; angle=$angle")
    }

    fun routeDone() {
        controller.stopCar()
    }

    fun move(moveDirection: MoveDirection, value: Int, callBack: () -> Unit) {
        //value - angle for rotation command and distance for forward/backward command
        this.moveDirection = moveDirection
        when (moveDirection) {
            MoveDirection.STOP -> controller.stopCar()
            MoveDirection.FORWARD -> controller.moveCarForward()
            MoveDirection.BACKWARD -> controller.moveCarBackward()
            MoveDirection.LEFT -> controller.moveCarLeft()
            MoveDirection.RIGHT -> controller.moveCarRight()
        }
        if (moveDirection != MoveDirection.STOP) {
            if (moveDirection == MoveDirection.FORWARD || moveDirection == MoveDirection.BACKWARD) {
                controller.delay(getTimeForMoving(value, MOVE_VELOCITY), callBack)
            } else {
                controller.delay(getTimeForMoving(value, ROTATION_VELOCITY), callBack)
            }
        }
    }

    fun getTimeForMoving(value: Int, velocity: Double): Int {
        return (1000 * Math.abs(value.toDouble()) / velocity).toInt()
    }
}