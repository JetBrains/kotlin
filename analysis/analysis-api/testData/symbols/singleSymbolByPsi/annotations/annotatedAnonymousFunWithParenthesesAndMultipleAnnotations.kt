annotation class A(val a: Int, val c: String)
annotation class B

fun foo() {
    val x = @A(42, "42") (@B label@ (fu<caret>n (){
            "Hi"
    }))
}
