class MyFileA
class MyFileB
class MyFileC

fun f(myFileB: MyFileB, myFileX: Int, myFileY: Int)
fun g(myFileY: Int)
fun h(myFileX: String)

fun foo(myFi<caret>)

// ORDER: myFileY
// ORDER: myFileB
// ORDER: myFileX
// ORDER: myFileX
// ORDER: myFileA
// ORDER: myFileC
