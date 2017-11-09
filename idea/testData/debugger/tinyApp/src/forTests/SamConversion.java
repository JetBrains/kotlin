package forTests;

public class SamConversion {
    public interface Runnable {
        public abstract void run();
    }

    public static void doAction(Runnable runnable) {
        runnable.run();
    }
}