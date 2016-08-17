

val PROGRAM_DURATION: Int = 1000

val ROTATE_TIME = 500
val STABILIZATION_TIME = 100

fun rotateLeftHack() {
    engine_turn_left()
    wait(ROTATE_TIME)
    engine_forward()
    wait(STABILIZATION_TIME)
    engine_backward()
    wait(STABILIZATION_TIME)
}

fun main() {
    init()
    echoProto()
}
