package test

class ShadeKotlin {
    val name2 = 1;
    fun inner() {
        val name2 = 2;
        print(this.name2)
        print(name2)
    }
}