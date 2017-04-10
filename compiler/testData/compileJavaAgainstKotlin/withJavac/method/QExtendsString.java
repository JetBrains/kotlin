package test;

class Question {
    // id2 is to prevent java type parameter type inference
    static <T> T id2(T p) { return p; }
    {
        String s = id2(QExtendsStringKt.id(null));
    }
}
