// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: VcsException.java
import org.jetbrains.annotations.NotNull;

public class VcsException extends Exception {
    @Override
    @NotNull
    public String getMessage() {
        return "";
    }
}

// FILE: main.kt
fun foo(e: VcsException) {
    e.message.contains("")
    "" in e.message
}

public operator fun CharSequence.contains(other: CharSequence): Boolean = true
