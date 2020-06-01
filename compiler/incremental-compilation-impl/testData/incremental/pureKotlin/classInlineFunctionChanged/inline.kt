package inline

class Klass {
    inline fun f(body: () -> Unit) {
        println("i'm inline function")
        body()
    }
}
