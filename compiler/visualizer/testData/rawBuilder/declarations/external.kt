external class External

external fun foo(): String

//           Int
//           │
external val x: Int

class NotExternal {
    external fun bar(): String
//      Int
//      │
    var y: Int
        external get
//          Int
//          │
        set(value) {}
}

//  Int
//  │
var z: Int
    external get
    external set
