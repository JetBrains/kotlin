
object Voyager {
    val VELOCITY_DRIVE: Double = 0.03 // centimeter in millisecond
    val VEL0CITY_ROUTATE: Double = 0.001 // degree in millisecond
    val SEGMENT_SIZE: Int = 30 // centimeter

    fun run() {
        while (true) {
            var distance = Sonar.getDistance(0)
            while (distance == -1 || distance > SEGMENT_SIZE) {
                drive(RouteType.FORWARD, SEGMENT_SIZE)
                distance = Sonar.getDistance(0)
            }

            rotate(Random.getInt())
        }
    }

    private fun rotate(degree: Int) {
        val time = (degree / VELOCITY_DRIVE).toInt()
        Engine.left()
        Time.wait(time)
    }

    private fun drive(direction: RouteType, distance: Int) {
        val time = (distance / VELOCITY_DRIVE).toInt()
        Engine.drive(direction.id)
        Time.wait(time)
    }
}