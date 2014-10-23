// checks that invalid characters (inserted e.g. by completion) inside single-line block do not cause wrong scopes for declarations below
fun foo() {
    x { v.s$ }
    val v = ""
}

fun bar() { }