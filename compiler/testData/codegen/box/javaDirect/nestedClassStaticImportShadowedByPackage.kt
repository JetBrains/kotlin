// TARGET_BACKEND: JVM
// ISSUE: KT-90357
// USE_PSI_JAVA_FACADE: false

// MODULE: decoy
// FILE: shp/Marker/Mode.kt
package shp.Marker

enum class Mode { VALUE, DECOY }

// MODULE: main(decoy)
// FILE: shp/Marker.kt
package shp

@Retention(AnnotationRetention.RUNTIME)
annotation class Marker(val value: Mode) {
    enum class Mode { FIRST, VALUE }
}

// FILE: shp/StaticSingleImportUser.java
package shp;

import static shp.Marker.Mode.VALUE;

public class StaticSingleImportUser {
    public static shp.Marker.Mode mode() {
        return VALUE;
    }
}

// FILE: shp/StaticOnDemandImportUser.java
package shp;

import static shp.Marker.Mode.*;

public class StaticOnDemandImportUser {
    public static shp.Marker.Mode mode() {
        return VALUE;
    }
}

// FILE: shp/FullyQualifiedUser.java
package shp;

public class FullyQualifiedUser {
    public static shp.Marker.Mode mode() {
        return shp.Marker.Mode.VALUE;
    }
}

// FILE: main.kt
import shp.FullyQualifiedUser
import shp.StaticOnDemandImportUser
import shp.StaticSingleImportUser

fun box(): String {
    if (StaticSingleImportUser.mode().name != "VALUE") return "FAIL: single static import"
    if (StaticOnDemandImportUser.mode().name != "VALUE") return "FAIL: static on-demand import"
    if (FullyQualifiedUser.mode().name != "VALUE") return "FAIL: qualified name"

    return "OK"
}
