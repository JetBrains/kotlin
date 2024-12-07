// RUN_PIPELINE_TILL: BACKEND
public fun fooNotNull(s: String) {
    System.out.println("Length of $s is ${s.length}")
}

public fun foo() {
    var s: String? = "not null"
    if (s == null) {
        return
    }
    fooNotNull(s)
}
