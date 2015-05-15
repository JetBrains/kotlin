open class Persistent(val p: String)
trait Hierarchy<T: Persistent > where T : Hierarchy<T>

class Location(): Persistent("OK"), Hierarchy<Location>

fun box(): String {
    return Location().p
}