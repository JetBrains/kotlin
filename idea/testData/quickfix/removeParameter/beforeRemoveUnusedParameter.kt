// "Remove unused parameter 'x'" "true"
class A {
    fun foo(<caret>x: Int, y: Int, z: Int) {
        foo(1, 2, 3);
        foo(1);
        foo(1, 2, 3, 4);
    }
}
