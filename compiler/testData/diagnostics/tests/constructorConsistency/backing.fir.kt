// IGNORE_REVERSED_RESOLVE
class My {
    var x = 1
        set(value) {
            field = value
        }

    var y: Int = 1
        set(value) {
            field = value + if (w == "") 0 else 1
        }

    var z: Int = 2
        set(value) {
            field = value + if (w == "") 1 else 0
        }

    var m: Int = 2
        set

    init {
        x = 3
        m = 6

        // Writing properties using setters is dangerous
        y = 4
        this.z = 5
    }

    val w = "6"
}