public fun test() {

    class Z {
        public fun localFun() {

        }
    }

    inline fun localFun2() {
        Z().localFun()
    }

}