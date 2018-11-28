package test

fun testJvm() {
    // This is a JVM 8 module, so `List.stream` should be resolved correctly
    getEmptyList().stream()
}
