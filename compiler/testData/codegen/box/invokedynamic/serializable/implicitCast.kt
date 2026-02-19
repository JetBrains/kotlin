// ISSUE: KT-70306
// TARGET_BACKEND: JVM
// SAM_CONVERSIONS: INDY
// LAMBDAS: INDY
// FULL_JDK

// FILE: JavaContainer.java
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

final class JavaContainer {
    public interface SFunction<R, I> extends Serializable
    {
        R apply (I i);
    }

    public static <R, I> byte[] serializeFunction(I i, SFunction<? extends R, ? super I> function)
    {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(function);
            oos.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
}

// FILE: box.kt

fun box(): String {
    val bytes = JavaContainer.serializeFunction(0) { 42 }
    return if (bytes.size != 0) "OK" else "FAIL"
}

