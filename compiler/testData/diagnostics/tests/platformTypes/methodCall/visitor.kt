// FILE: p/Visitor.java

package p;

public interface Visitor<D> {

}

// FILE: p/Element.java

package p;

public class Element {
    public <D> R accept(@NotNull Visitor<D> visitor, D data) {return null;}
}

// FILE: k.kt

import p.*

fun test(v: Visitor<Nothing>, e: Element) {
    e.accept(v, null)
}