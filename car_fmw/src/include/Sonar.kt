external fun car_sonar_init()
external fun car_sonar_get_dist(degree: Byte): Short

object Sonar {
    private var SCAN_STEP = 1

    fun init() {
        car_sonar_init()
    }

    fun getDistance(angle: Int): Int =
            car_sonar_get_dist(angle.toByte()).toInt()

    fun getSmoothDistance(angle: Int, windowSize: Int): Int {
        val distance = getDistance(angle)
        if (distance != -1 || windowSize == 0) {
            return distance
        }

        val start = if (angle - windowSize < 0) 0 else angle - windowSize
        return getOneOfRange(start, angle + windowSize, SCAN_STEP)
    }

    fun getOneOfRange(start: Int, stop: Int, step: Int): Int {
        var i = start
        var distance = getDistance(start)
        while (i <= stop && distance == -1) {
            distance = getDistance(i)
            i += step
        }

        while (i >= start && distance == -1) {
            distance = getDistance(i)
            i -= step
        }

        return distance
    }

    fun calibrate() {
        getDistance(180)
    }
}