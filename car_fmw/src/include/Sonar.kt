external fun car_sonar_init()
external fun car_sonar_get_dist(degree: Byte): Short

object Sonar {
    fun init() {
        car_sonar_init()
    }

    fun getDistance(angle: Int): Int =
            car_sonar_get_dist(angle.toByte()).toInt()

    fun getSmoothDistance(angle: Int, windowSize: Int, smoothing: SonarRequest.Smoothing): Int {
        val distance = getDistance(angle)
        if (distance != -1 || windowSize == 0) {
            return distance
        }

        val start = if (angle - windowSize < 0) 0 else angle - windowSize
        val stop = angle + windowSize
        when (smoothing.id) {
            SonarRequest.Smoothing.NONE.id -> {
                return getOneOfRange(start, stop)
            }
            SonarRequest.Smoothing.MEDIAN.id -> {
                val data = getRange(start, stop).filter(::positive)
                if (data.size == 0) {
                    return -1
                }

                return data.median()
            }
            SonarRequest.Smoothing.MEAN.id -> {
                val data = getRange(start, stop).filter(::positive)
                if (data.size == 0) {
                    return -1
                }

                return data.mean()
            }
        }

        return -1
    }

    private fun getOneOfRange(start: Int, stop: Int): Int {
        var i = start
        var distance = getDistance(start)
        while (i <= stop && distance == -1) {
            distance = getDistance(i)
            i += 1
        }

        while (i >= start && distance == -1) {
            distance = getDistance(i)
            i -= 1
        }

        return distance
    }

    private fun getRange(start: Int, stop: Int): IntArray {
        val result = IntArray(stop - start + 1)
        var i = 0
        while (i < stop - start + 1) {
            result[i] = getDistance(start + i)
            i++
        }

        return result
    }


    fun calibrate() {
        getDistance(180)
    }
}