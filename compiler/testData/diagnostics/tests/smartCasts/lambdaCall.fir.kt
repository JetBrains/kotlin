// See KT-9134: smart cast is not provided inside lambda call
fun bar(): Int = <!UNRESOLVED_REFERENCE!>{
    var i: Int?
    i = 42
    i
}()<!>