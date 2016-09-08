
external fun dynamic_heap_tail(): Int
external fun static_heap_tail(): Int
external fun dynamic_heap_max_bytes(): Int
external fun dynamic_heap_total(): Int

object DebugInfo {
    fun getDynamicHeapTail(): Int = dynamic_heap_tail()
    fun getStaticHeapTail(): Int = static_heap_tail()
    fun getDynamicHeapMaxSize(): Int = dynamic_heap_max_bytes()
    fun getDynamicHeapTotalBytes(): Int = dynamic_heap_total()
}

external fun car_sonar_get_measurement_total(): Int
external fun car_sonar_get_measurement_failed_checksum(): Int
external fun car_sonar_get_measurement_failed_distance(): Int
external fun car_sonar_get_measurement_failed_command(): Int

object DebugSonarInfo {
    fun getMeasurementCount(): Int = car_sonar_get_measurement_total()
    fun getMeasurementFailedChecksum(): Int = car_sonar_get_measurement_failed_checksum()
    fun getMeasurementFailedDistance(): Int = car_sonar_get_measurement_failed_distance()
    fun getMeasurementFailedCommand(): Int = car_sonar_get_measurement_failed_command()
}
