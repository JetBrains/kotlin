// TARGET_BACKEND: JVM

// MODULE: annoArray
// FILE: Anno.java
public @interface Anno {
    Class<?>[] value();
}

// MODULE: vc
// FILE: vc.kt
@JvmInline
value class VC(val s: String)

// MODULE: jholder(annoArray, vc)
// FILE: JHolder.java
@Anno(String.class)
public class JHolder {
    public String use(VC v) {
        return "OK";
    }
}

// MODULE: annoScalar
// FILE: Anno.java
public @interface Anno {
    Class<?> value();
}

// MODULE: main(annoScalar, jholder, vc)
// FILE: main.kt
fun box(): String = JHolder().use(VC("OK"))
