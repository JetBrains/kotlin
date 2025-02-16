annotation class Ann

context(_: String)
@Ann
fun foo1() {}

@Ann
context(_: String)
fun foo2() {}

private
context(_: String)
inline
@Ann
context(_: String)
public
fun foo3() {}

context(_: String)
@Ann
val foo1: String get() = ""

@Ann
context(_: String)
val foo2: String get() = ""

private
context(_: String)
inline
@Ann
context(_: String)
public
val foo3: String get() = ""