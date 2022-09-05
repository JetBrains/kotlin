// IGNORE_BACKEND: JS

open class A(val a: String = DEFAULT_A){
    companion object: A(){
        const val DEFAULT_A = "O"
    }
}

open class B(val b: String = DEFAULT_B){
    companion object: B(){
        const val DEFAULT_B = "K"
    }
}

fun box() = A.a + B().b