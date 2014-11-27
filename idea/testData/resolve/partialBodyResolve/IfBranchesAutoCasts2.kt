fun foo(p: Any?, p1: Any?, p2: Any?) {
    if (x()) {
        print(p!!)
        print(p1!!)
    }
    else {
        print(p1 as String)
        print(p2!!)
    }

    <caret>p1.hashCode()
}