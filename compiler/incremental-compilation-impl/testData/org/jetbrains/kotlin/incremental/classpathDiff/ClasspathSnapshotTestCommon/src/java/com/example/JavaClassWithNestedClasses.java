package com.example;

public class JavaClassWithNestedClasses {

    public class InnerClass {

        public int publicField = 0;

        private int privateField = 0;

        public void publicMethod() {
            System.out.println("I'm in a public method");
        }

        private void privateMethod() {
            System.out.println("I'm in a private method");
        }

        public class InnerClassWithinInnerClass {
        }

        public void someMethod() {

            class LocalClassWithinInnerClass {
            }
        }
    }

    public static class StaticNestedClass {
    }

    public void someMethod() {

        class LocalClass {

            class InnerClassWithinLocalClass {
            }
        }

        Runnable objectOfAnonymousLocalClass = new Runnable() {
            @Override
            public void run() {
            }
        };
    }

    private Runnable objectOfAnonymousNonLocalClass = new Runnable() {
        @Override
        public void run() {
        }
    };

    public class InnerClassWith$Sign {
    }
}
