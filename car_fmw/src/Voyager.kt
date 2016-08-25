
object Voyager {
    val VELOCITY_DRIVE: Double = 0.05 // centimeter in millisecond
    val VEL0CITY_ROTATE: Double = 0.05 // degree in millisecond
    val SEGMENT_SIZE: Int = 30 // centimeter

    val MAX_ANGLE: Int = 180 // degree
    val ENGINE_DELAY: Int = 500 // milisecond

    fun run() {
        while (true) {
            var distance = getSmoothDistance(0)
            while (distance == -1 || distance > 4 * SEGMENT_SIZE) {
                drive(RouteType.FORWARD, SEGMENT_SIZE)
                distance = getSmoothDistance(0)
            }

            rotate(45)
        }
    }

    private fun rotate(degree: Int) {
        val duration = (degree.toDouble() / VEL0CITY_ROTATE).toInt()
        Engine.left()
        Time.wait(duration)
        smoothStop()
    }

    private fun drive(direction: RouteType, distance: Int) {
        val duration = (distance.toDouble() / VELOCITY_DRIVE).toInt()
        Engine.drive(direction.id)
        Time.wait(duration)
        smoothStop()
    }

    private fun getSmoothDistance(degree: Int): Int {
        Sonar.getDistance(180)
        return Sonar.getDistance(degree)
    }

    private fun smoothStop() {
        Engine.stop()
        Time.wait(ENGINE_DELAY)
    }
}