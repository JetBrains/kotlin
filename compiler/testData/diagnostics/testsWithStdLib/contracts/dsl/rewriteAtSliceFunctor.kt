// See KT-28847

class Foo(val str: String?) {
    val first = run {
        str.isNullOrEmpty()
        second
    }

    val second = str.isNullOrEmpty()
}