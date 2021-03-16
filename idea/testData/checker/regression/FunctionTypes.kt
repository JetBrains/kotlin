// FIR_IDENTICAL

fun <K, V> intercept(<warning>block</warning>: (key: K, next: (K) -> V, K) -> V) {
}

fun <T : (Int) -> String> f() {

}

fun <T> g() where T: (unit: Unit) -> Unit {

}

class C<T : (Int) -> String>() {

}

class CC<T>() where T : (Int) -> String {

}

interface I<T>

val c = object : I<(String) -> String> {}

class CCC() : I<(Int) -> Int>
