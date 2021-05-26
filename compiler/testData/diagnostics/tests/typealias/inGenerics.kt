// FIR_IDENTICAL
typealias MyString = String

class Container<T>(val x: T)

typealias MyStringContainer = Container<MyString?>

val ms: MyString = "MyString"

val msn: MyString? = null

val msc: MyStringContainer = Container(ms)
val msc1 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyStringContainer<!>(null)
