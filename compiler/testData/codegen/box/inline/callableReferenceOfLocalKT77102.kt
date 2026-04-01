// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization

// FILE: lib.kt
inline fun <T> inlineGenericTestFunction(f: () -> T) = f()

// FILE: main.kt
val updateStatus = inlineGenericTestFunction {
    fun <F> bangbang(flag: F) = flag!!
}

fun box() = "OK"
