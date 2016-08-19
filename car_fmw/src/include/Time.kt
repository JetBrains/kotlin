
external fun car_time_init()
external fun car_time_wait(msec: Int)
external fun car_time_get_timestamp(): Int

object Time {
    fun init() {
        car_time_init()
    }

    fun wait(msec: Int) {
        car_time_wait(msec)
    }

    fun getTimestamp(): Int = car_time_get_timestamp()
}