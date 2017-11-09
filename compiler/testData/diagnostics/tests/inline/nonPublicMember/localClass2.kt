public fun test() {

    class Z {
        public fun localFun() {

        }
    }

    <!NOT_YET_SUPPORTED_IN_INLINE!>inline<!> fun localFun2() {
        Z().localFun()
    }

}