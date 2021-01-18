fun main() {
    // Module java.naming
    val b: javax.naming.Binding? = null
    println(b)

    // Module jdk.net
    val j: jdk.net.Sockets? = null
    println(j)

    // Module jdk.httpserver (this module doesn't depend on it)
    val s: com.sun.net.httpserver.HttpServer? = null
    println(s)
}
