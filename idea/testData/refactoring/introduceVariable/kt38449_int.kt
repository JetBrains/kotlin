fun b(body: () -> Int) = body()

class A {
    fun test() = b {
        <selection>24</selection>
    }
}