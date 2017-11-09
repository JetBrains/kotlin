fun foo(vararg strings: String, option: String = ""){ }

fun bar(s: String){
    foo("", "", <caret>)
}

// EXIST: { lookupString:"option =", itemText:"option =" }
