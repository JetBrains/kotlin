// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects, +AllowExpectValueClassesWithNoPrimaryConstructor
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi
// MODULE: common
// FILE: common.kt

@WillBecomeValue
expect class WithoutOverrides

@WillBecomeValue
expect class WithOverrides

// MODULE: platform()()(common)
// FILE: JWithoutOverrides.java

@kotlin.WillBecomeValue
public final class JWithoutOverrides {
    public JWithoutOverrides(int x) {}
}

// FILE: JWithOverrides.java

@kotlin.WillBecomeValue
public final class JWithOverrides {
    public JWithOverrides(int x) {}

    @Override public boolean equals(Object other) { return other instanceof JWithOverrides; }
    @Override public int hashCode() { return 0; }
    @Override public String toString() { return "JWithOverrides"; }
}

// FILE: platform.kt

actual typealias <!IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS!>WithoutOverrides<!> = JWithoutOverrides

actual typealias WithOverrides = JWithOverrides

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, javaType, typeAliasDeclaration */
