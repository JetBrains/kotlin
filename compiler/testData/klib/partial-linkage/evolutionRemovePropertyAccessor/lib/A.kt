val bar 
    get() = "original global value of val"
var muc = "initialized global value of var with field"
    get() = field
    set(value) {
        field = "original global value of var with field"
    }
var toc 
    get() = "original global value of var without field"
    set(value) { }


class X() {
    val qux 
        get() = "original member value of val"
    var nis = "initialized member value of var with field"
        get() = field
        set(value) {
            field = "original member value of var with field"
        }
    var roo = "initialized member value of var without field"
        get() = "original member value of var without field"
}

