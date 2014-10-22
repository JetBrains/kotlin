//KT-5854 Incorrect 'Nested class should be qualified'

class A {
    class Nested {

    }

    fun foo() {
        Nested(<!TOO_MANY_ARGUMENTS!>1<!>) //two errors here, the first one is wrong
    }
}
