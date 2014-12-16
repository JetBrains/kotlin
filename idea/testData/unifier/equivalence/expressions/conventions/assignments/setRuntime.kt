// DISABLE-ERRORS
val array = Array(2) { it }

class Foo {
    {
        <selection>array[1] = 10</selection>
        array.set(1, 10)
        array2.set(1, 10)
    }
}