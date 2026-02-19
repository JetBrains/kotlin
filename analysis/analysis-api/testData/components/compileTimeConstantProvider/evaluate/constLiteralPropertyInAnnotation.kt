annotation class MyAnno(val x: Int)

object Something {
    const val VALUE: Int = 5
}

@MyAnno(<expr>Something.VALUE</expr>)
fun foo() {}