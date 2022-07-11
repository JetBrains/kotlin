open class A(val a: String = DEFAULT_A){
    companion object: A(){
        const val DEFAULT_A = "OK"
    }
}

fun box() = A().a