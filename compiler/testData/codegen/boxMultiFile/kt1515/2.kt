package otherpackage

fun fromOtherPackage(): Boolean {
    val c = Runnable::class.java
    return (c.getName()!! == "java.lang.Runnable")
}