package test

fun String./*rename*/length(asdf: String) {

}

fun Any.length() { // won't be renamed

}

fun String.length(i: Int) {

}

class X {
    fun String.length() { // won't be renamed

    }
}