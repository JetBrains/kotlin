package dependency

operator fun <T> List<T>.component1(): T = get(0)
operator fun <T> List<T>.component2(): T = get(1)
operator fun <T> List<T>.component3(): T = get(2)