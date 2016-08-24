
object Voyager {
    val VELOCITY_DRIVE: Double = 0.03 // centimeter in millisecond
    val VEL0CITY_ROUTATE: Double = 0.015 // degree in millisecond
    val SEGMENT_SIZE: Int = 30 // centimeter

    val MAX_ANGLE: Int = 180 // degree
    val ENGINE_DELAY: Int = 500 // milisecond

    fun run() {
        while (true) {
            var distance = Sonar.getDistance(0)
            while (distance == -1 || distance > SEGMENT_SIZE) {
                drive(RouteType.FORWARD, SEGMENT_SIZE)
                distance = Sonar.getDistance(0)
            }

            rotate(Random.getInt() % MAX_ANGLE)
        }
    }

    private fun rotate(degree: Int) {
        val duration = (degree.toDouble() / VELOCITY_DRIVE).toInt()
        Engine.left()
        Time.wait(duration)
    }

    private fun drive(direction: RouteType, distance: Int) {
        val duration = (distance.toDouble() / VELOCITY_DRIVE).toInt()
        Engine.drive(direction.id)
        Time.wait(duration)
        smoothStop()
    }

    private fun smoothStop() {
        Engine.stop()
        Time.wait(ENGINE_DELAY)
    }
}