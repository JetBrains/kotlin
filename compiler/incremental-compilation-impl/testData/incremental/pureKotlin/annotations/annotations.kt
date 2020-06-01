package test

annotation class Anno

@Anno fun f() {
}

@Anno val v1 = ""

var v2: String
    get() = ""
    @Anno set(value) {
    }
