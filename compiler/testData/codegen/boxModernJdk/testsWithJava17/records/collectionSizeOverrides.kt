// API_VERSION: 1.5
// LANGUAGE: +JvmRecordSupport
// ENABLE_JVM_PREVIEW

@JvmRecord
data class MyRec(override val size: Int) : Collection<String> {
    override fun contains(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<String> {
        TODO("Not yet implemented")
    }
}

fun box(m: MyRec, c: Collection<*>): String {
    val c: Collection<*> = m
    if (m.size != 56) return "fail 1"
    if (c.size != 56) return "fail 2"
    return "OK"
}

fun box(): String {
    val m = MyRec(56)
    return box(m, m)
}
