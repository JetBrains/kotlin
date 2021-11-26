// FILE: GoBuildingRunningState.java
public abstract class GoBuildingRunningState<T extends GoBuildingRunConfiguration<? extends GoBuildingRunningState<T>>> implements RunProfileState {
    public String getBuildingWorkingDirectory() {
        return "";
    }
}

// FILE: GoBuildingRunConfiguration.java
public abstract class GoBuildingRunConfiguration<RunningState extends GoBuildingRunningState<? extends GoBuildingRunConfiguration<RunningState>>> { }

// FILE: RunProfileState.java
public interface RunProfileState {
}

// FILE: Test.java
import org.jetbrains.annotations.Nullable;

public class Test {
    @Nullable
    public RunProfileState getState() {
        return null;
    }
}

// FILE: main.kt
fun String.asFsdAddress(): String {
    return ""
}

fun box(): String {
    val state = Test().state
    if (state is GoBuildingRunningState<*>) {
        <!DEBUG_INFO_SMARTCAST, TYPE_MISMATCH_WARNING!>state<!>.buildingWorkingDirectory.asFsdAddress()
    }
    return "OK"
}
