import static foo.OverloadKt.overloadNew;

public class OverloadJava {
    public void useOverload() {
        overloadNew(0, false);
        overloadNew(0, true);
        overloadNew(0, true, 2.0);
    }
} 