// !DIAGNOSTICS: -UNUSED_VARIABLE
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

object Elvis {
    fun fromJava() {
        val a = jsr.string ?: ""
        val b = jsrNullable?.string ?: ""

        val c = jb.string <!USELESS_ELVIS!>?: ""<!>
        val d = jbNullable?.string ?: ""

        val e = platform.string ?: ""
        val f = platformNullable?.string ?: ""
    }

    fun toJava(nullableString: String?) {
        val b = jsr.consumeString(nullableString ?: "")
        val d = jsrNullable?.consumeString(nullableString ?: "")

        val f = jb.consumeString(nullableString ?: "")
        val h = jbNullable?.consumeString(nullableString ?: "")

        val j = platform.consumeString(nullableString ?: "")
        val l = platformNullable?.consumeString(nullableString ?: "")
    }
}
