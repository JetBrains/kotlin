fun foo(list: List<String>){}
fun foo(list: List<String>, i: Int){}
fun foo(list: List<Int>, b: Boolean){}
fun foo(list: List<Int>, c: Char){}

fun f(){
    foo(empty<caret>)
}

// EXIST: { lookupString: "emptyList", typeText: "List<String>" }
// EXIST: { lookupString: "emptyList", typeText: "List<Int>" }
// NOTHING_ELSE: true
