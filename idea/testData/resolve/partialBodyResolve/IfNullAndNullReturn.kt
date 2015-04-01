fun foo(p1: Any?, p2: Any?) {
    if (p1 == null && p2 == null) {
        print("2 null's")
        return
    }
    <caret>p1?.hashCode()
}