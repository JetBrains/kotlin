// FILE: JavaImpl.java

public class JavaImpl implements KotlinInterface {
    private final String id;

    JavaImpl(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}

// FILE: JavaAnnotation.java

public @interface JavaAnnotation {
    public Class<?> as() default Void.class;
}

// FILE: kt25489.kt
// TARGET_BACKEND: JVM
// Note: if you wish to reproduce KT-25489, turn UL classes OFF

@JavaAnnotation(`as` = JavaImpl::class)
interface KotlinInterface : Identity

interface Identity {
    fun id(): String
}

fun box(): String {
    val impl = JavaImpl("OK")
    return impl.id()
}