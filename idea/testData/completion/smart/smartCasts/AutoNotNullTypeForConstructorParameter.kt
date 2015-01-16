class Foo(val s: String?) {
    fun bar(){
        if (s != null) {
            foo(<caret>)
        }
    }
}

fun foo(s: String){}

// EXIST: { itemText:"s" }
