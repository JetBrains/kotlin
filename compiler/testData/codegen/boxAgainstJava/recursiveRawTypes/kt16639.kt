// WITH_RUNTIME
// FILE: Device.java
import java.util.Collection;

public class Device<D extends Device, S extends Service> {
    public static Collection<Device<Device, Service>> getLoop() {
        return null;
    }
}

// FILE: Service.java

public class Service<D extends Device, S extends Service> {
}

// FILE: loop.kt

fun box(): String {
    val x = Device.getLoop()?.firstOrNull() // compilation error
    return "OK"
}
