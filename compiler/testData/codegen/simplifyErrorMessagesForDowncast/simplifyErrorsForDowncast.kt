package foo;

class A(val name: String) {

    fun printName(any: Any?) {
        (any as A).name
    }

}
