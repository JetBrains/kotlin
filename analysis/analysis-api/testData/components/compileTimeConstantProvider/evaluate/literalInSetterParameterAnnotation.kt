annotation class Anno(val s: String)

var x: Int = 1
    set(@Anno(<expr>"a"</expr>) value) {
        field = value
    }
