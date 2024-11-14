// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-56863
interface I

open class Some {
    val x: Int

    init {
        this as I
        x = 1
    }
}
