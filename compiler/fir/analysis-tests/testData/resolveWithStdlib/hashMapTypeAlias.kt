typealias MyT<X> = HashMap<X, Int>

fun <X> MyT<X>.add(x: X, y: Int) {}

fun main() {
    MyT<String>().apply {
        add("1", 2)
    }
}
