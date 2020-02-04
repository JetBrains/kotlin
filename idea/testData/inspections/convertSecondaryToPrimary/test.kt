class Single {

    val x: Int

    constructor(x: Int) {
        this.x = x
    }
}

class NotSingle {

    val x: Int

    constructor(): this(42)

    constructor(x: Int) {
        this.x = x
    }
}

// KT-22142
class Person {
    constructor(email: String) {
        this.email = email
    }

    var email: String
        set(value) {
            require(value.isNotBlank(), { "The email cannot be blank" })
            field = value
        }
}