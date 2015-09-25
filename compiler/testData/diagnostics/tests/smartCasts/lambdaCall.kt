// See KT-9134: smart cast is not provided inside lambda call
fun bar(): Int = {
    var i: Int?
    i = 42
    <!DEBUG_INFO_SMARTCAST!>i<!>
}()