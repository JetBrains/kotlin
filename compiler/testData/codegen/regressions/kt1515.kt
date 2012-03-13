fun box(): String {
    val c = javaClass<Runnable>()
    return if(c.getName().sure() == "java.lang.Runnable") "OK" else "fail"
}