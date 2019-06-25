object ForceSam : java.util.Comparator<Runnable> {
    override fun compare(o1: Runnable, o2: Runnable): Int = 0
}

fun test(r: Runnable) {
    ForceSam.compare(r, r)
    ForceSam.compare({}, {})

    ForceSam.compare(r, {})
    ForceSam.compare({}, r)
}
