typealias MyString = String

class Container<T>(val x: T)

typealias MyStringContainer = Container<MyString?>

val ms: MyString = "MyString"

val msn: MyString? = null

val msc: MyStringContainer = Container(ms)
val msc1 = MyStringContainer(null)