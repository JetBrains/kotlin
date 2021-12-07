fun test() {
    class A
    @Denotable("A") A()
    @Denotable("kotlin.collections.List<A>") listOf(A())
    @Nondenotable("<anonymous>") object {}
    @Nondenotable("kotlin.collections.List<<anonymous>>") listOf(object {})
}
