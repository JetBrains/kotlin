// SKIP_TXT
// FIR_IDENTICAL
// FILE: Generic.java
import java.util.List;

public class Generic<T extends Generic> {
    // Returns Raw type
    public static Generic create() { return null; }
    public List<String> getFoo() { return null; }

    public T getChild() { return null; }
    public List<Generic> getChildren() { return null; }
}

// FILE: main.kt
fun main() {
    val generic = Generic.create() // has a type of Generic<(raw) Any..Any?>

    generic.getFoo() // has return type List<(raw) Any..Any?>
    generic.getFoo()[0].<!UNRESOLVED_REFERENCE!>length<!> // Unresolved "length"
    generic.foo[0].length // OK

    generic.child.children[0].foo[0].length // OK
}
