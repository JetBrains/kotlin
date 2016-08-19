import control.RouteExecutor
import control.emulator.RouteExecutorImpl.MoveDirection

private val MOVE_VELOCITY = 0.3278
private val ROTATION_VELOCITY = 12.3

// TODO make Car class mutable state saving entity
// that almost doesn't have behavior
/**
 * Created by user on 7/27/16.
 */
class CarState private constructor() {
    //position
    var x: Double
    var y: Double
    var angle: Double

    init {
        this.x = 0.0
        this.y = 0.0
        this.angle = 0.0
    }

    companion object {
        val instance = CarState()
    }
}