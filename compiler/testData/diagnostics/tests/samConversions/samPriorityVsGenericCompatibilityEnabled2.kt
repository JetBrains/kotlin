// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +DisableCompatibilityModeForNewInference
// FULL_JDK

import java.util.function.Consumer

class Exec

fun <T> register(vararg constructorArgs: Any): String = "1" // (1)
fun <T> register(configurationAction: Consumer<in T>): Int = 2 // (2)

fun withConversion(): (Exec) -> Unit = {}
fun withoutConversion() = Consumer<Exec> {}

fun main() {
    val x = register<Exec>(withConversion()) // K2 -> (1)
    acceptInt(x)

    val y = register<Exec>(withoutConversion()) // K2 -> (2)
    acceptInt(y)
}

fun acceptInt(i: Int) {}
