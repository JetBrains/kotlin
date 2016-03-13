class A(private var v1: String) {
    private var v2 = v1
    override fun toString(): String { return "A[v1=$v1,v2=$v2]" }
}