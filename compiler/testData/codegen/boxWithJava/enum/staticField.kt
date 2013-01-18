import test.staticField as E

fun box(): String {
    val instances = E.INSTANCES
    if (E.foo != 42)
        return "Wrong foo ${E.foo}"
    if (instances.size() != 1)
        return "Wrong size ${instances.size()}"
    if (E.INSTANCES.iterator().next() != E.INSTANCE)
        return "Wrong instance ${E.INSTANCES.iterator().next()}"
    return "OK"
}
