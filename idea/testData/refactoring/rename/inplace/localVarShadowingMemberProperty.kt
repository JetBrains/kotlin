package test

class ShadeKotlin {
    val name1 = 1;
    fun inner() {
        val <caret>name2 = 2;
        print(name1)
        print(name2)
    }
}