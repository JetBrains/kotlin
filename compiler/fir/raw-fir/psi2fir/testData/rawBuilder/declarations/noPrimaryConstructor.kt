// IGNORE_TREE_ACCESS: KT-65268
class NoPrimary {
    val x: String

    constructor(x: String) {
        this.x = x
    }

    constructor(): this("")
}