// FALSE
// Important for 173 branch! OOCB is TRUE in this test because of IDEA-185462

class B(val a: A)
val B.foo: Int
    get() {
        return <caret>when (a) {
            A.e1 -> 1
            A.e2 -> 4
        }
    }