class Foo(val prop1 : String?, val prop2 : String?){
    fun f(p1: Foo, p2: Foo) {
        if (p1.prop1 != null && p2.prop2 != null && prop2 != null){
            var a : String = p1.<caret>
        }
    }
}

// EXIST: { itemText:"prop1" }
// ABSENT: { itemText:"prop2" }
// EXIST: { itemText:"!! prop2" }
// EXIST: { itemText:"?: prop2" }
