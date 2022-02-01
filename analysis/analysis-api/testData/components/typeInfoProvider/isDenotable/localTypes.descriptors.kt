fun test() {
    class A
    @Denotable("A") A()
    @Denotable("kotlin.collections.List<A>") listOf(A())
    @Nondenotable("`<no name provided>`") object {}
    @Nondenotable("kotlin.collections.List<`<no name provided>`>") listOf(object {})
}
