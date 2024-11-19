// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: MyRunnable.java
public interface MyRunnable {
    void run();
}

// FILE: Computable.java
public interface Computable<T> {
    T compute();
}

// FILE: ThrowableComputable.java
public interface ThrowableComputable<T, E extends Throwable> {
    T compute() throws E;
}

// FILE: Application.java

public interface Application {
    void runWriteAction(MyRunnable action);

    <T> T runWriteAction(Computable<T> computation);

    /**
     * Runs the specified computation in a write-action. Must be called from the Swing dispatch thread.
     * The action is executed immediately if no read actions or write actions are currently running,
     * or blocked until all read actions and write actions complete.
     * <p>
     * See also {@link WriteAction#compute} for a more lambda-friendly version.
     *
     * @param computation the computation to run
     * @return the result returned by the computation.
     * @throws E re-frown from ThrowableComputable
     * @see CoroutinesKt#writeAction
     */
    @SuppressWarnings("LambdaUnfriendlyMethodOverload")
    @RequiresBlockingContext
    <T, E extends Throwable> T runWriteAction(ThrowableComputable<T, E> computation) throws E;
}
