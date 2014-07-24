package test

public class Input(val s1: String, val s2: String) {
    public fun iterator() : Iterator<String> {
        return arrayListOf(s1, s2).iterator()
    }
}

public inline fun <T, R> T.use(block: (T)-> R) : R {
    return block(this)
}

public inline fun Input.forEachLine(block: (String) -> Unit): Unit {
    useLines { lines -> lines.forEach(block) }
}

public inline fun Input.useLines(block2: (Iterator<String>) -> Unit): Unit {
        this.use{ block2(it.iterator()) }
}

