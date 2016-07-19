// YES for first two, NO for last two expected

var x: String = ""
    set(param: String) {
        field = "$param "
    }

class My {
    var y: Int = 1
        set(param: Int) {
            field = param - 1
        }

    var z: Double = 3.14
        private set

    var w: Boolean = true
        set(param) {
            field = !param
        }
}