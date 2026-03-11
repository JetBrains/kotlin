// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
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

// MODULE: main(lib)
// FILE: loop.kt

fun box(): String {
    val x = Device.getLoop()?.firstOrNull() // compilation error
    return "OK"
}
