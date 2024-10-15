// RUN_PIPELINE_TILL: SOURCE
public fun test() {

    class Z {
        public fun localFun() {

        }
    }

    <!NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION!>inline<!> fun localFun2() {
        Z().localFun()
    }

}