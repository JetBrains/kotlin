class Box<T>() {
    open inner class Inner () {
        fun isT(s : Any) = if(s is T) "OK" else "fail"
    }

    inner class Inner2() : Inner() {
    }

    fun inner() = Inner2()
}

fun box(): String {
  return Box<String>().inner().isT("OK")
}
