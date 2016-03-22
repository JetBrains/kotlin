public class Getter {
    public static void foo(k.Class c) {
        c.get<caret>Prop();
    }
}

// REF: (in k.Class).prop
// CLS_REF: (in k.Class).prop
