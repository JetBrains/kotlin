package otherpackage

fun fromOtherPackage(): Boolean {
    val c = javaClass<Runnable>()
    return (c.getName()!! == "java.lang.Runnable")
}