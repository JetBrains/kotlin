public class PackageFacade {
    public static void foo() {
        k.Dep<caret>endenciesKt.topLevelFunction()
    }
}

// REF: /src/<test dir>/resolve/referenceInJava/dependency.dependencies.kt
// CLS_REF: <jar>!/k.DependenciesKt.class