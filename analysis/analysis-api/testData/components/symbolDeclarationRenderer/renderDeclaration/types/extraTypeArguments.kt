import AAA.BBB

public interface AAA<T> {
    class BBB : AAA<Int>
}


fun foo() : BBB<*> {
}