package hello

fun main() {
    val systemClassLoader = ClassLoader.getSystemClassLoader()
    val moduleName = "META-INF/test.kotlin_module"
    val resourceAsStream = systemClassLoader.getResourceAsStream(moduleName)

    if (resourceAsStream != null) {
        println("Module info '$moduleName' exists")
    }
}
