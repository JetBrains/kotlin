public fun test() {

    fun localFun() {

    }

    <!NOT_YET_SUPPORTED_IN_INLINE!>inline fun localFun2() {
        localFun()
    }<!>

}