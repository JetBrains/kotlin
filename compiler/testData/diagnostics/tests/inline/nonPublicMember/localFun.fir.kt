public fun test() {

    fun localFun() {

    }

    inline fun localFun2() {
        localFun()
    }

}