// WITH_MESSAGE: "Removed 4 imports, added 1 import"
import duration.*
import duration.hours as hour
import duration.minutes as minute
import duration.minutes as minutes
import duration.seconds as second
import duration.seconds as seconds

fun main() {
    val totalDuration = 1.hour - 30.minutes + 1.second
    println(totalDuration)
}