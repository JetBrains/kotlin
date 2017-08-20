// WITH_RUNTIME

val b: Sample by lazy {
    object : Sample {   }
}

private val withoutType by lazy {
    object : Sample { }
}

interface Sample

fun box(): String {
    b
    withoutType
    return "OK"
}