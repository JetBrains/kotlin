import k.*;

public class Test {
    public static void bar(TraitWithDelegatedNoImpl some) {
        some.<caret>foo();
    }
}

// REF: (in k.TraitNoImpl).foo()
