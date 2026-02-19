annotation class MyAnno(val message: String) {
    annotation class List(vararg val value: MyAnno)
}

@MyAnno.List(<expr>MyAnno(message = )</expr>)
fun foo() {}

