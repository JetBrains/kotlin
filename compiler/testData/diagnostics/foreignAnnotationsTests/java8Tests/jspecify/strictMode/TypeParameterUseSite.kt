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

// FILE: ClsMarkedNullable.java
import org.jspecify.annotations.*;

@NullMarked
public class ClsMarkedNullable<T extends @Nullable Object> {
    public T go(T t) { return t; }
    public ClsMarkedNullable<T> self() { return this; }
}

// FILE: ClsMarked.java
import org.jspecify.annotations.*;

@NullMarked
public class ClsMarked<T> {
    public T go(T t) { return t; }
    public ClsMarked<T> self() { return this; }
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

fun testNonnull(x: ClsMarkedNullable<String>, nullable: String?, nonnull: String) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(<!TYPE_MISMATCH!>nullable<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(nonnull)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("ClsMarkedNullable<kotlin.String>")!>x.self()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.self().go(<!TYPE_MISMATCH!>nullable<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.self().go(nonnull)<!>
}

fun testNullable(x: ClsMarkedNullable<String?>, nullable: String?, nonnull: String) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.go(nullable)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.go(nonnull)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("ClsMarkedNullable<kotlin.String?>")!>x.self()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.self().go(nullable)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.self().go(nonnull)<!>
}

fun testNonnull(x: ClsMarked<String>, nullable: String?, nonnull: String) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(<!TYPE_MISMATCH!>nullable<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.go(nonnull)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("ClsMarked<kotlin.String>")!>x.self()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.self().go(<!TYPE_MISMATCH!>nullable<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x.self().go(nonnull)<!>
}

fun testNullable(x: ClsMarked<<!UPPER_BOUND_VIOLATED!>String?<!>>, nullable: String?, nonnull: String) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.go(nullable)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.go(nonnull)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("ClsMarked<kotlin.String?>")!>x.self()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.self().go(nullable)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.self().go(nonnull)<!>
}
