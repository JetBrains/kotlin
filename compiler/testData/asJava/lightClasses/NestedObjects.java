public final class A {
    public A() { /* compiled code */ }

    public static final class B {
        public B() { /* compiled code */ }

        public static final class I {
            @org.jetbrains.annotations.NotNull
            public static final A.B.I INSTANCE;

            private I() { /* compiled code */ }
        }

        public static final class II {
            @org.jetbrains.annotations.NotNull
            public static final A.B.II INSTANCE;

            private II() { /* compiled code */ }
        }
    }

    public static final class C {
        @org.jetbrains.annotations.NotNull
        public static final A.C INSTANCE;

        private C() { /* compiled code */ }

        public static final class D {
            @org.jetbrains.annotations.NotNull
            public static final A.C.D INSTANCE;

            private D() { /* compiled code */ }

            public static final class G {
                @org.jetbrains.annotations.NotNull
                public static final A.C.D.G INSTANCE;

                private G() { /* compiled code */ }
            }
        }
    }
}