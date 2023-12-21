// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS
// !LANGUAGE: -SkipStandaloneScriptsInSourceRoots
// WITH_STDLIB

// Although this test works in K1 just fine, it is named with the suffix K2 to show that the demonstrated method is the only one
// available so far to call a script from another module.
// In K1 one can do it directly, but this is not the functionality we want to retain

// MODULE: lib
// FILE: script.kts

fun ok() = "OK"

// MODULE: main(lib)
// FILE: test.kt

fun runScriptMethod(name: String, method: String): Any {
    val klass = Thread.currentThread().contextClassLoader.loadClass(name)
    val constructor = klass.constructors.single()
    val instance = constructor.newInstance(emptyArray<String>())
    val method = klass.getMethod(method)
    return method.invoke(instance)
}

fun box(): String =
    runScriptMethod("Script", "ok") as String
