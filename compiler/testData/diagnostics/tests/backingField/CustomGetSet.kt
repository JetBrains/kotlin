class Flower() {

    var minusOne: Int = 1
        get() = $minusOne + 1
        set(n: Int) { $minusOne = n - 1 }

}
