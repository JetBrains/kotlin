// FILE: A.java

import org.checkerframework.checker.nullness.qual.*;
import java.util.*;

class A {
    List<@NonNull String> foo() {}
}
