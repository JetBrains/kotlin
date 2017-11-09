// (failed) attempt to reproduce exception in
// http://stackoverflow.com/questions/42571812/unsupportedoperationexception-while-building-a-kotlin-project-in-idea

// FILE: Fun.java
public interface Fun<T> {
    public void invoke(T x);
}

// FILE: A.java
public interface A<T> {
    public void foo(T x, Fun<? extends T> y);
}

// FILE: B.java
public interface B extends A {
    @Override
    public void foo(Object x, Fun y);
}

// FILE: C.java
public abstract class C {
    public abstract <T> void bar(Fun<T> y);

    public static <T> void aStaticMethod(T x, Fun<T> y) {}

    public static abstract class D extends C {
        @Override
        public <T> void bar(Fun<T> y) {}

        public static void aStaticMethod(Object x, Fun y) {}
    }
}

// FILE: main.kt
class E1 : C.D(), B {
    override fun foo(x: Any, y: Fun<Any?>) {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class E2<!> : B {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(x: Any, y: Fun<String?>) {}
}
