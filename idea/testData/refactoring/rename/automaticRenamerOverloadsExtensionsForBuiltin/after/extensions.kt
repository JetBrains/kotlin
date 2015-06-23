package test

fun String./*rename*/foo(asdf: String) {

}

fun Any.length() { // won't be renamed

}

fun String.foo(i: Int) {

}

class X {
    fun String.length() { // won't be renamed

    }
}