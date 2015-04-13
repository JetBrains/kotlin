class Foo(val prop1 : Any, val prop2 : Any){
    fun f(p1: Foo, p2: Foo) {
        if (p1.prop1 is String && p2.prop2 is String && prop2 is String){
            var a : String = p1.<caret>
        }
    }
}

// EXIST: { itemText:"prop1" }
// ABSENT: { itemText:"prop2" }
