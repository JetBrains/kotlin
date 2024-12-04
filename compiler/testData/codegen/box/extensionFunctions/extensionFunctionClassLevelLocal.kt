fun foo(): String {
    class Local<T> {
        fun Local<String>.bar(): String {
            return "OK"
        }
    }
    with(Local<String>()){
        return Local<String>().bar()
    }
}

fun box(): String = foo()