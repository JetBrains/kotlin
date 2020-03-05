// !DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: Function.java

public interface Function<Param, Result> {
    Result fun(Param param);
}

// FILE: AdapterProcessor.java

public class AdapterProcessor<T, S> {
    public AdapterProcessor(Function<? super T, ? extends S> conversion) {}
}


// FILE: main.kt

interface PsiMethod {
    val containingClass: PsiClass?
}

interface PsiClass

fun test() {
    // TODO: don't forget to implement preservation flexibility of java type parameters in FIR (this is the reason of error here)
    val processor = AdapterProcessor<PsiMethod, PsiClass>(
        Function { method: PsiMethod? -> method?.containingClass }
    )
}