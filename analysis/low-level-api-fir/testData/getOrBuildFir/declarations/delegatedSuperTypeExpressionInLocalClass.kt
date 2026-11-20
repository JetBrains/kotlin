interface MyInterface

fun foo(i: MyInterface) {
    class Local : MyInterface by <expr>i</expr>
}
