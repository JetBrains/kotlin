import k.*;

public class Test {
    public static void bar(TraitWithDelegatedWithImpl some) {
        some.<caret>foo();
    }
}

// REF: (in k.TraitWithImpl).foo()
