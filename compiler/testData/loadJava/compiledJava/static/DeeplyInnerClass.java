package test;

public class DeeplyInnerClass {
    class A {
        void a() {}
        class B {
            void b() {}
            class C {
                void c() {}
            }
        }
    }
}