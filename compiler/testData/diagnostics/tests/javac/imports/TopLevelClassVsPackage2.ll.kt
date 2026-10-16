// LL_FIR_DIVERGENCE
// LL tests always use the PSI Java facade and ignore `USE_PSI_JAVA_FACADE: false`. The PSI Java facade resolves
// the import from the longest package prefix, so the results match TopLevelClassVsPackage2Psi.kt (KT-90484)
// LL_FIR_DIVERGENCE
// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: FRONTEND
// USE_PSI_JAVA_FACADE: false
// java-direct classifies `foo.a` in `import foo.a.b` as the class `foo.a` (JLS 6.5.4.2) and looks up `b` among its member
// types, never in the package `foo.a`. The PSI Java facade still picks the class `b` of the package `foo.a`, see
// TopLevelClassVsPackage2Psi.kt and KT-90484

// FILE: foo/a/b.java
package foo.a;

public class b {

    public void a_b() {}

    public class c {
        public void a_bc() {}
    }
}

// FILE: foo/a.java
package foo;

public class a {

    public void _a() {}

    public class b {
        public void _ab() {}
    }

}

// FILE: foo/c.java
package foo;

import foo.a.b;

public class c {
    public b getB() { return null; }
}

// FILE: foo/c2.java
package foo;

public class c2 {
    public a.b getB() { return null; }
}

// FILE: e.kt
package foo

fun test() = c().getB().c().a_bc()
fun test2() = c2().getB()._ab()

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
