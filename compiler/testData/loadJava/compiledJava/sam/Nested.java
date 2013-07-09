package test;

public interface Nested {

    public interface Runnable {
        void run();
    }

    public interface Deeper1 {
        public interface Runnable {
            void run();
            void run2();
        }
    }

    public interface Deeper2 {
        public interface Runnable {
            void run();
            String toString();
        }
    }
}
