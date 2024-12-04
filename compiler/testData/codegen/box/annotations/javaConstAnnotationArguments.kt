// FIR_IDENTICAL
// ISSUE: KT-57879
// TARGET_BACKEND: JVM_IR

// FILE: CoreBundle.java

public class CoreBundle {
    public static final String BUNDLE = "OK";
}

// FILE: main.kt

@Target(AnnotationTarget.TYPE)
annotation class AnnKlass(val argument: String)

fun message(key: @AnnKlass(CoreBundle.BUNDLE) String) = key

fun box() = message("OK")
