public fun test() {

    fun localFun() {

    }

    <!NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION!>inline<!> fun localFun2() {
        localFun()
    }

}