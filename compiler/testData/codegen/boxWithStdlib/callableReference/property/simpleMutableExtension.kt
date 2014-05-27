var storage = 0

var Int.foo: Int
    get() {
        return this + storage
    }
    set(value) {
        storage = this + value
    }

fun box(): String {
    val pr = Int::foo
    if (pr.get(42) != 42) return "Fail 1: ${pr[42]}"
    pr.set(200, 39)
    if (pr.get(-239) != 0) return "Fail 2: ${pr[-239]}"
    if (storage != 239) return "Fail 3: $storage"
    return "OK"
}
