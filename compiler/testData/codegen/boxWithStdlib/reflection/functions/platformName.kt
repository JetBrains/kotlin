import kotlin.platform.platformName

@platformName("Fail")
fun OK() {}

fun box() = ::OK.name
