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

    init {
        // Writing properties using setters is dangerous
        <!DEBUG_INFO_LEAKING_THIS!>y<!> = 4
        this.<!DEBUG_INFO_LEAKING_THIS!>z<!> = 5
    }

    val w = "6"
}