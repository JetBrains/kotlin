// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// FILE: J.java
public interface J {
    Object foo();
}

// FILE: 1.kt
private lateinit var x: Any

fun foo(j: J) {
    <!PLATFORM_TYPE_ASSIGNMENT_TO_LATEINIT!>x = j.foo()<!>
}