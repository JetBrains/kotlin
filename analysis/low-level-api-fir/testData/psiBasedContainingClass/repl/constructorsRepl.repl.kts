class WithPrimaryConstructor(val x: Int)

class WithSecondaryConstructor(val x: Int) {
    constructor(x: Int, y: Int) : this(x)
}