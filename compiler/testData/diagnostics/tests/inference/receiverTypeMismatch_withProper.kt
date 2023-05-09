// LANGUAGE: +ProperTypeInferenceConstraintsProcessing

// FILE: Configuration.java
public class Configuration<S extends State<? extends Configuration<S>>> {
    public String getDirectoryPath() { return ""; }
}

// FILE: State.java
public class State<C extends Configuration<? extends State<C>>> {}

// FILE: Main.kt
fun setup(configuration: Configuration<*>) {
    configuration.apply {
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>directoryPath<!>
    }
}
