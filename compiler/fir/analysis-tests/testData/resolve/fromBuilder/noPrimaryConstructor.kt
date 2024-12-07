// RUN_PIPELINE_TILL: BACKEND
class NoPrimary {
    val x: String

    constructor(x: String) {
        this.x = x
    }

    constructor(): this("")
}
