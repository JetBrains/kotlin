// RUN_PIPELINE_TILL: BACKEND
// JLS 9.7.4 (`@C int @A [] @B [] f`, transliterated here as `@Elem String @Outer [] @Inner []`): each
// annotation belongs to one level — the one written before the type name annotates the element type,
// the leftmost bracket pair is the outermost array — and the vararg `...` is simply the rightmost
// dimension (JLS 10.2). A Java view which scans the whole type syntactically instead of per level
// propagates every annotation to every level. `f4`/`f5` pin the nullability-visible half of that: a
// `@NotNull` in the member's modifier list is also a declaration annotation, so it makes the declared
// type (the array, whatever its number of dimensions) non-null and leaves the nested levels flexible.

// FILE: Elem.java
import java.lang.annotation.*;

@Target({ElementType.TYPE_USE})
public @interface Elem {}

// FILE: Outer.java
import java.lang.annotation.*;

@Target({ElementType.TYPE_USE})
public @interface Outer {}

// FILE: Inner.java
import java.lang.annotation.*;

@Target({ElementType.TYPE_USE})
public @interface Inner {}

// FILE: J.java
import org.jetbrains.annotations.*;

public class J {
    public @Elem String f1;
    public @Elem String @Outer [] f2;
    public @Elem String @Outer [] @Inner [] f3;
    public @NotNull String [] f4;
    public @NotNull String [] [] f5;
}

// FILE: test.kt
fun test(j: J) {
    <!DEBUG_INFO_EXPRESSION_TYPE("(@Elem() kotlin.String..@Elem() kotlin.String?)")!>j.f1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(@Outer() kotlin.Array<(@Elem() kotlin.String..@Elem() kotlin.String?)>..@Outer() kotlin.Array<out (@Elem() kotlin.String..@Elem() kotlin.String?)>?)")!>j.f2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(@Outer() kotlin.Array<(@Inner() kotlin.Array<(@Elem() kotlin.String..@Elem() kotlin.String?)>..@Inner() kotlin.Array<out (@Elem() kotlin.String..@Elem() kotlin.String?)>?)>..@Outer() kotlin.Array<out (@Inner() kotlin.Array<(@Elem() kotlin.String..@Elem() kotlin.String?)>..@Inner() kotlin.Array<out (@Elem() kotlin.String..@Elem() kotlin.String?)>?)>?)")!>j.f3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<(kotlin.String..kotlin.String?)>..kotlin.Array<out (kotlin.String..kotlin.String?)>)")!>j.f4<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<(kotlin.Array<(kotlin.String..kotlin.String?)>..kotlin.Array<out (kotlin.String..kotlin.String?)>?)>..kotlin.Array<out (kotlin.Array<(kotlin.String..kotlin.String?)>..kotlin.Array<out (kotlin.String..kotlin.String?)>?)>)")!>j.f5<!>
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaProperty, javaType */
