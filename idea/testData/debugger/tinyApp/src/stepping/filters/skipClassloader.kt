package skipClassloader

fun main(args: Array<String>) {
    val loader = javaClass<A>().getClassLoader()!!
    try {
        //Breakpoint!
        val aaa = loader.loadClass("skipClassloader.A")
    }
    catch (e: ClassNotFoundException) {
        e.printStackTrace()
    }

}

class A

// TRACING_FILTERS_ENABLED: false
// SKIP_CLASSLOADERS: true
