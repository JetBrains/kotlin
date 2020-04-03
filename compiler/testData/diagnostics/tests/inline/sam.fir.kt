// !DIAGNOSTICS: -NON_LOCAL_RETURN_NOT_ALLOWED
// FILE: Run.java
public interface Run {
    public int run();
}


// FILE: Test.java
public class Test {
    public void test(Run r) {

    }
}

// FILE: test.kt
inline fun inlineFunWithInvoke(s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {
    Test().test(){
        s(11)
        s.invoke(11)
        s invoke 11

        11.ext(11)
        11 ext 11

        s
        ext
        11
    }
}

inline fun inlineFunWithInvokeNonInline(noinline s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {
    Test().test(){
        s(11)
        s.invoke(11)
        s invoke 11

        11.ext(11)
        11 ext 11

        s
        ext

        11
    }
}

inline fun Function1<Int, Unit>.inlineExt() {
    Test().test(){
        invoke(11)
        this.invoke(11)
        this invoke 11
        this(11)

        this

        11
    }
}