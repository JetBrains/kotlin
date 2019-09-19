package test;

public class JavaClass {
    public Task task(Action<? super Task> configureAction) {
        return new Task();
    }
}