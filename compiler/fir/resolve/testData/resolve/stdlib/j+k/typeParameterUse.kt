// FULL_JDK
// FILE: U.java
import org.jetbrains.annotations.NotNull;
public class U {

    @NotNull
    public <T> T getValue(Box<@NotNull T> box) {}
}

// FILE: Box.java

public class Box<T> {}


// FILE: useSite.kt
fun foo(holder: U, box: Box<Int>): Int {

    return holder.getValue(box)
}