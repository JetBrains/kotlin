// FIR_IDENTICAL
// WITH_STDLIB
// DUMP_CFG
// ISSUE: KT-53898

class Test {
    private var size: Int
    private val values: List<String>

    init {
        this.size = 0
    }

    constructor(map: Map<String, String>) : this(map.values.map { it }) {
        this.size += this.values.size
    }

    constructor(set: Set<String>) : this(set.map { it }) {
        this.size += this.values.size
    }

    private constructor(list: List<String>) {
        this.values = list
    }
}
