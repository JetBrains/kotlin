external class External

external fun foo(): String

external val x: Int

class NotExternal {
    external fun bar(): String
    var y: Int
        external get
        set(value) {}
}

var z: Int
    external get
    external set
