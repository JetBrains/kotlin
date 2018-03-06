public class Constructor {
    public static void foo() {
        new k.<caret>class();
    }
}

// REF: (k).Class
// CLS_REF: (k).Class
