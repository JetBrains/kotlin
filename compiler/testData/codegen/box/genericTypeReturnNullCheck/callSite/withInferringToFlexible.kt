// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME


fun box(): String {
    val x = listOf<String>("")
    println(x[0])
    return "OK"
}
