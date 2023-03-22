// IGNORE_REVERSED_RESOLVE
class Some(var foo: Int) {
    init {
        if (foo < 0) {
            foo = 0
        }
    }

    val y = run {
        foo = 1
        foo
    }

    constructor(): this(-1) {
        foo = 2
    }
}
