import <!DEPRECATION!>C<!> as C2

@Deprecated("obsolete")
class C {
    fun use() {}
}

fun useAlias(c : <!DEPRECATION!>C2<!>) { c.use() }
