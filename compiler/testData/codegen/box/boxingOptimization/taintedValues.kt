// WITH_RUNTIME

// Just make sure there's no VerifyError

fun getOrElse() =
        mapOf<String, Int>().getOrElse("foo") { 3 }

fun isNotEmpty(l: ArrayList<Int>) =
        l.iterator()?.hasNext() ?: false

fun box() = "OK"