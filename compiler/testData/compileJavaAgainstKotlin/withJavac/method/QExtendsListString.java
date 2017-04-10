package test;

class Question {
    // id2 is to prevent java type parameter type inference
    static <T> T id2(T p) { return p; }
    {
        java.util.List<? extends String> s = id2(QExtendsListStringKt.id(null));
    }
}
