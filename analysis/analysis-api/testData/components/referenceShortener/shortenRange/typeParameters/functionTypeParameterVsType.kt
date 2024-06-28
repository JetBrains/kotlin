package test

interface MyType

<expr>
context(test.MyType)
fun <MyType> test.MyType.test(p: test.MyType) {
    val a: test.MyType
}
</expr>