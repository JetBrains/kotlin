
object Voyager {
    private val MAX_ANGLE: Int = 180 // angle
    private val SEGMENT_SIZE: Int = 30 // centimeter
    private var DEFAULT_WINDOW = 10 // angle

    fun run() {
        while (true) {
            var distance = Sonar.getSmoothDistance(0, DEFAULT_WINDOW, SonarRequest.Smoothing.NONE)
            while (distance == -1 || distance > 4 * SEGMENT_SIZE) {
                Engine.drive(RouteType.FORWARD.id, SEGMENT_SIZE)
                distance = Sonar.getSmoothDistance(0, DEFAULT_WINDOW, SonarRequest.Smoothing.NONE)
            }

            Engine.drive(RouteType.LEFT.id, 45)
        }
    }
}