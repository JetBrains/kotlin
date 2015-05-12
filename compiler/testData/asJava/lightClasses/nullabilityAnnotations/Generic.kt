// Generic

interface Generic<N, NN: Any> {
    fun a(n: N): N
    fun b(nn: NN): NN

    fun a1(n: N?): N?
    fun b1(nn: NN?): NN?
}