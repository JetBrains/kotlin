import foo.OverloadKt;

import static foo.OverloadKt.overload;

public class OverloadJava {
    public void useOverload() {
        OverloadKt.overloadNew(0, false);
        OverloadKt.overloadNew(0, true);
        OverloadKt.overloadNew(0, true, 2.0);
        overload("123");
    }
} 