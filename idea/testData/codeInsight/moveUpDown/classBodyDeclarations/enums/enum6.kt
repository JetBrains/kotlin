// MOVE: down
// IS_APPLICABLE: false
// class A
enum class A {
    // U
    U,
    // V
    V;

    // class B
    enum class B {
        // X
        X,
        // Y
        <caret>Y
    }
}