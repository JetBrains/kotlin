fun foo(c: Collection<String>): Collection<String> {
    return <caret>c.filter {
        val v = it.length
        val v1 = v * v
        if (v1 > 10) {
            true
        }
        else {
            println()
            it[0] == 'a'
        }
    }
}