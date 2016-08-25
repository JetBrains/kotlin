
object Voyager {

    val MAX_ANGLE: Int = 180 // degree
    val SEGMENT_SIZE: Int = 30 // centimeter

    fun run() {
        while (true) {
            var distance = Sonar.getSmoothDistance(0)
            while (distance == -1 || distance > 4 * SEGMENT_SIZE) {
                Engine.drive(RouteType.FORWARD.id, SEGMENT_SIZE)
                distance = Sonar.getSmoothDistance(0)
            }

            Engine.rotate(RouteType.LEFT.id, 45)
        }
    }
}