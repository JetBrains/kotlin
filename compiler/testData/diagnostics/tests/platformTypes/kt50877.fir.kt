// FULL_JDK
// WITH_STDLIB

// FILE: Schematic.kt
class Schematic {
    var name: String? = null

    var error: String? = null

    override fun toString(): String {
        return name!!
    }
}

// FILE: SortedListModel.java
import java.util.Comparator;

public class SortedListModel<T> {
    public SortedListModel(Comparator<? super T> comparator) {
    }
}


// FILE: main.kt
val model = SortedListModel<Schematic>(Comparator.comparing { b1: Schematic ->
    when {
        b1.error != null -> 2
        b1.name!!.contains(":") -> 1
        else -> 0
    }
}.thenComparing { b1: Schematic -> b1.name!! })