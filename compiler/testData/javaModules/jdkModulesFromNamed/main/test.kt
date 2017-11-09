fun main(args: Array<String>) {
    // Module java.naming
    val b: javax.naming.Binding? = null
    println(b)

    // Module jdk.net
    val j: jdk.net.Sockets? = null
    println(j)

    // Module jdk.httpserver (this module doesn't depend on it)
    val s: com.sun.net.httpserver.HttpServer? = null
    println(s)

    // Module oracle.desktop
    val a: com.oracle.awt.AWTUtils? = null
    println(a)

    // No module, this class is declared in $JDK_9/lib/ant-javafx.jar
    val x: com.sun.javafx.tools.ant.AntLog? = null
    println(x)
}
