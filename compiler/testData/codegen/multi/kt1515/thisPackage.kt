package thispackage

import otherpackage.*

fun box(): String {
    if (!localUse()) {
        return "local use failed"
    }
    if (!fromOtherPackage()) {
        return "use from other package failed"
    }
    return "OK"
}

fun localUse(): Boolean {
    val c = javaClass<Runnable>()
    return (c.getName()!! == "java.lang.Runnable")
}