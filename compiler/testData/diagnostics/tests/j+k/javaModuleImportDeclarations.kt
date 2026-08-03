// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-84499
// WITH_STDLIB
// JDK_KIND: FULL_JDK_25

// FILE: ExampleParent.java
import module java.base;

public class ExampleParent {
    public Set<String> examples() {
        return Set.of("hello", "world");
    }
}

// FILE: main.kt
class ExampleChild : ExampleParent() {
    override fun examples(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Set<String><!> {
        return setOf("hello", "kotlin")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, override, stringLiteral */
