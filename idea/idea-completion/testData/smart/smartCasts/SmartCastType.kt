fun f(p: Any) {
    if (p is String){
        var a : String = <caret>
    }
}

// EXIST: { itemText:"p" }
