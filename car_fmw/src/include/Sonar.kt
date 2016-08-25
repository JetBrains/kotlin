external fun car_sonar_init()
external fun car_sonar_get_dist(degree: Byte): Short

object Sonar {
    fun init() {
        car_sonar_init()
    }

    fun getDistance(degree: Int): Int = car_sonar_get_dist(degree.toByte()).toInt()

    fun getSmoothDistance(degree: Int): Int {
        Sonar.getDistance(180)
        return Sonar.getDistance(degree)
    }

}