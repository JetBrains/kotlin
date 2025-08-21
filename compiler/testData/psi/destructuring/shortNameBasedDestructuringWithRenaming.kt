// COMPILATION_ERRORS

fun declaration() {
    val (aa = first) = x
    val (aa: String = first) = x
}

fun loop() {
    for ((aa = first) in x) {}
    for ((aa: String = first) in x) {}
}

fun lambda() {
    foo { (aa = first) -> }
    foo { (aa: String = first) -> }
}