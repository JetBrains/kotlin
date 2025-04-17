// JSPECIFY_STATE: strict
// ISSUE: KT-73658

// FILE: ClsNonNull.java
import org.jspecify.annotations.*;

public class ClsNonNull<T extends @NonNull Object> {
    public T go(T t) { return t; }
}

// FILE: ClsUnmarkedNonNull.java
import org.jspecify.annotations.*;

@NullUnmarked
public class ClsUnmarkedNonNull<T extends @NonNull Object> {
    public T go(T t) { return t; }
}

// FILE: ClsMarkedUnmarked.java
import org.jspecify.annotations.*;

@NullMarked
public class ClsMarkedUnmarked<T> {
    @NullUnmarked
    public T go(T t) { return t; }
}

// FILE: main.kt
fun test(x: ClsNonNull<String>, nullable: String?, nonnull: String) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(<!TYPE_MISMATCH!>nullable<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(nonnull)<!>
}

fun test(x: ClsUnmarkedNonNull<String>, nullable: String?, nonnull: String) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(<!TYPE_MISMATCH!>nullable<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(nonnull)<!>
}

fun test(x: ClsMarkedUnmarked<String>, nullable: String?, nonnull: String) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(<!TYPE_MISMATCH!>nullable<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(nonnull)<!>
}
