// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses

fun box(): String {
    VArray(1){0}.clone()
    return "OK"
}