// FIR_IDENTICAL
// WITH_STDLIB

// FILE: WorkAction.java
public interface WorkAction<T> {
    T getParameters();
}

// FILE: main.kt
abstract class RunGTestJob : WorkAction<RunGTestJob.Parameters> {
    interface Parameters {
        val executable: String
    }

    fun execute() {
        with(parameters) {
            <!VAL_REASSIGNMENT!>executable<!> = executable
        }
    }
}
