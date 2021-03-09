// FULL_JDK

// FILE: LongRunningOperation.java
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public interface LongRunningOperation {
    LongRunningOperation setEnvironmentVariables(@Nullable Map<String, String> envVariables);
}

// FILE: ConfigurableLauncher.java
import java.util.Map;

public interface ConfigurableLauncher<T extends ConfigurableLauncher<T>> extends LongRunningOperation {
    @Override
    T setEnvironmentVariables(Map<String, String> envVariables);
}

// FILE: BuildActionExecuter.java
public interface BuildActionExecuter<T> extends ConfigurableLauncher<BuildActionExecuter<T>> {}

// FILE: main.kt

fun <T> test(executor: BuildActionExecuter<T>, modelType: Class<T>, env: Map<String, String>) {
    val model = executor.setEnvironmentVariables(env)
}
