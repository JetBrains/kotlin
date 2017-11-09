
import kotlin.collections.MapsKt;
import kotlin.internal.PlatformImplementations; // Internal in stdlib, error
import java.util.function.Consumer;


public class SomeClass extends PlatformImplementations { // Internal in stdlib, error
    public static void doSomething() {
        PlatformImplementations a; // Internal in stdlib, error
        Integer c;
        MapsKt.mapCapacity(3); // Internal in stdlib, error
        Consumer<Integer> fun = MapsKt::mapCapacity; // Internal in stdlib, error
        SomeInternalClass b = new SomeInternalClass(); // Internal in same module, OK

        SomeInternalClassInOtherModule b = new SomeInternalClassInOtherModule(); // Internal in other module, error
    }
}