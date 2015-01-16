fun foo(list: List<String>){}

fun f(){
    foo(<caret>)
}

// EXIST: { lookupString: "listOf", tailText: "() (kotlin)", typeText: "List<String>" }
// EXIST: { lookupString: "listOf", tailText: "(vararg values: String) (kotlin)", typeText: "List<String>" }
// EXIST: { lookupString: "arrayListOf", tailText: "(vararg values: String!) (kotlin)", typeText: "ArrayList<String!>" }
