fun box(): String {
    val c = javaClass<Runnable>()
    return if(c.getName()!! == "java.lang.Runnable") "OK" else "fail"
}