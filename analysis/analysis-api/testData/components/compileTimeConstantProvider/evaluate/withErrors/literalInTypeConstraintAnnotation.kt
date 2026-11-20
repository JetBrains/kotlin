annotation class Anno(val s: String)

fun <T> f() where @Anno(<expr>"a"</expr>) T : Any {}
