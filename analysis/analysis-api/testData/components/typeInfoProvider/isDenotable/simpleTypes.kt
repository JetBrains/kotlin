interface A
fun test(a: A) {
    @Denotable("kotlin.Int") 1
    @Denotable("kotlin.String") ""
    Denotable("A") a
}
