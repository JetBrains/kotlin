fun foo(x: Int?) {
    // Both parts of the Elvis should be alive, see KT-7936
    x?.let {
        smth()
    }?: orElse()
}

fun smth() {}
fun orElse() {}