annotation class A(val a: Int, val c: String)

interface MyInterface

val x = @A(42, "") object : MyInterface {<caret>}
