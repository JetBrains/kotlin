import C as C2

@Deprecated("obsolete")
class C {
    fun use() {}
}

fun useAlias(c : C2) { c.use() }
