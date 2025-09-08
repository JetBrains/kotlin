fun test() {
    fun localFun() {
        println("localFun()")
    }

    val newName = <expr>::localFun</expr>
    newName()
}