package test

sealed class Test {
    object O : Test()

    class Extra(val x: Int): Test
}