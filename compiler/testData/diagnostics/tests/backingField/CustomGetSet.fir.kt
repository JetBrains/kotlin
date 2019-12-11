class Flower() {

    var minusOne: Int = 1
        get() = field + 1
        set(n: Int) { field = n - 1 }
}
