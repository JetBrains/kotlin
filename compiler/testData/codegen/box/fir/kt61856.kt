// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// MODULE: a
// FILE: Email.java
package javax.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Email {
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface List {
        Email[] value();
    }
}

// FILE: a.kt
import javax.validation.constraints.Email

class BoardContentLogController {
    fun getBoardContentItemLogs(@Email.List emails: List<String>) {}
}

// MODULE: box(a)
// FILE: box.kt
fun box(): String {
    BoardContentLogController().getBoardContentItemLogs(listOf("email1", "email2"))
    return "OK"
}