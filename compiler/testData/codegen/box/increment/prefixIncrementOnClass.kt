trait Base
class Derived: Base
class Another: Base
fun Base.inc(): Derived { return Derived() }

public fun box() : String {
    var i : Base
    i = Another()
    val j = ++i

    return if (j is Derived && i is Derived) "OK" else "fail j = $j i = $i"
}
