
object ContextClassLoaderTester {

    @JvmStatic
    fun main(args: Array<String>) {
        kotlin.test.DefaultAsserter.assertTrue("", true) // this tests that kotlin-test is in the compilation and runtime classpaths
        val contextClassLoader = Thread.currentThread().getContextClassLoader()
        contextClassLoader.loadClass("kotlin.test.DefaultAsserter") // this tests that thread context classloader is set correctly
        println("ok")
    }
}
