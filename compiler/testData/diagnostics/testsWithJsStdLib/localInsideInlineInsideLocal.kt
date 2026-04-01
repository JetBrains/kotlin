// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78976, KT-79013
// DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun topLevelInlineFun() {
    <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> localFun() {}
    localFun()

    <!NOT_YET_SUPPORTED_IN_INLINE!>inline<!> fun localInlineFun() {
        <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> localFun() {}
        localFun()
    }
    localInlineFun()
}

fun topLevelFun() {
    inline fun localInlineFun() {
        fun localFun() {}
        localFun()
    }
    localInlineFun()
}

fun main() {
    topLevelInlineFun()
    topLevelFun()
}
