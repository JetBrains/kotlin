fun foo(array: Array<String>){}

fun f(){
    foo(<caret>)
}

// EXIST: { lookupString: "array", tailText: "(vararg t: String) (kotlin)", typeText: "Array<String>" }
// ABSENT: arrayOfNulls
