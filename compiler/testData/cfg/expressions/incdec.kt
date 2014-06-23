fun bar(n: Int) {

}

fun foo() {
    var a = 1
    bar(a++)
    bar(a--)
    bar(++a)
    bar(--a)
}