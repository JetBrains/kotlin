// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC

// Two type-import-on-demand declarations bring the same simple name `Widget` into scope from
// different on-demand targets (JLS 7.5.2):
//
//   import p1.*;          // package-on-demand  -> resolves `Widget` to the top-level p1.Widget
//   import p2.Holder.*;   // class-on-demand     -> resolves `Widget` to the member type p2.Holder.Widget
//
// Per JLS 7.5.2 / 6.5.5.1 a simple name reachable through two different on-demand imports is
// ambiguous (`javac` reports "reference to Widget is ambiguous"), so the Java type `Widget` does
// not resolve and the cross-language call below is left unresolved.
//
// `UserOk` is the positive control: with only the class-on-demand import in scope, the class-level
// fallback resolves `Widget` to p2.Holder.Widget unambiguously, so `okWidget()` resolves.

// FILE: p1/Widget.java
package p1;

public class Widget {
    public int fromPackage() { return 1; }
}

// FILE: p2/Holder.java
package p2;

public class Holder {
    public static class Widget {
        public int fromHolder() { return 2; }
    }
}

// FILE: q/UserOk.java
package q;

import p2.Holder.*;

public class UserOk {
    public Widget getWidget() { return null; }
}

// FILE: q/UserAmbiguous.java
package q;

import p1.*;
import p2.Holder.*;

public class UserAmbiguous {
    public Widget getWidget() { return null; }
}

// FILE: main.kt
package q

fun okWidget() = UserOk().getWidget().fromHolder()

fun ambiguousWidget() = UserAmbiguous().<!MISSING_DEPENDENCY_CLASS!>getWidget<!>()

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
