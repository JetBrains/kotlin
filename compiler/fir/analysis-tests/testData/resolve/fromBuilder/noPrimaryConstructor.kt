class NoPrimary {
    val x: String

    constructor(x: String) {
        this.x = x
    }

    constructor(): this("")
}
