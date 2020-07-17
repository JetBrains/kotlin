fun b(body: () -> Unit): Unit = body()

class A {
    fun test() = b {
        <selection>24</selection>
    }
}