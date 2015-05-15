@[]
class A {
    @[] val x = 1

    @[@q]
    fun foo() {
        @[] class A
    }

    @[@q1 @ @q2]
    fun foo2() {}

    @[
    fun bar() {}
}
