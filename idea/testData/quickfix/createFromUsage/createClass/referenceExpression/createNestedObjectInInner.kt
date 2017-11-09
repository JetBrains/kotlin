// "Create object 'Nested'" "true"
class A {
    // TARGET_PARENT:
    inner class B {
        val a = <caret>Nested
    }
}