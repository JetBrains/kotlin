// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION
// JSR305_GLOBAL_REPORT warn

// FILE: MyNotNull.java
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.PARAMETER})
@Nonnull
@TypeQualifierNickname
@Retention(RetentionPolicy.RUNTIME)
public @interface MyNotNull {}


// FILE: AnnotatedWithJsr.java
public class AnnotatedWithJsr {
    @MyNotNull
    public String getString() {
        return null;
    }

    public void consumeString(@MyNotNull String s) { }
}



// FILE: AnnotatedWithJB.java
import org.jetbrains.annotations.NotNull;

public class AnnotatedWithJB {
    public @NotNull String getString() {
        return "hello";
    }

    public void consumeString(@NotNull String s) { }
}


// FILE: PlainJava.java
public class PlainJava {
    public String getString() {
        return null;
    }

    public void consumeString(String s) { }
}


// FILE: main.kt
val jsr: AnnotatedWithJsr = AnnotatedWithJsr()
val jsrNullable: AnnotatedWithJsr? = null

val jb: AnnotatedWithJB = AnnotatedWithJB()
val jbNullable: AnnotatedWithJB? = null

val platform: PlainJava = PlainJava()
val platformNullable: PlainJava? = null

val a = jsr.string
val b = jsrNullable?.string
val c = jb.string
val d = jbNullable?.string
val e = platform.string
val f = platformNullable?.string

fun evlis() {
    // JSR
    val r1 = a ?: ""
    val r2 = b ?: ""

    // JB
    val r3 = c <!USELESS_ELVIS!>?: ""<!>
    val r4 = d ?: ""

    // Platform
    val r5 = e ?: ""
    val r6 = f ?: ""
}

fun ifChecksAndSmartCasts() {
    // JSR
    val r1 = if (<!SENSELESS_COMPARISON, SENSELESS_COMPARISON!>a == null<!>) 42 else a.length
    val r2 = if (b == null) 42 else <!DEBUG_INFO_SMARTCAST!>b<!>.length

    // JB
    val r3 = if (<!SENSELESS_COMPARISON!>c == null<!>) 42 else c.length
    val r4 = if (d == null) 42 else <!DEBUG_INFO_SMARTCAST!>d<!>.length

    // Platform
    val r5 = if (e == null) 42 else e.length
    val r6 = if (f == null) 42 else <!DEBUG_INFO_SMARTCAST!>f<!>.length
}