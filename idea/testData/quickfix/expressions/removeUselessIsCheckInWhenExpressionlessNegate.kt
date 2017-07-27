// "Remove useless is check" "true"

interface Base
interface Derived: Base

fun foo(bar: Base):Int {
    return when {
        bar is Derived -> 0
        bar <caret>!is Base -> 42
        else -> 1
    }
}