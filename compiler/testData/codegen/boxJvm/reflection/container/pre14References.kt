// WITH_REFLECT

// FILE: J.java
public class J {
    public void foo() {}
}

// FILE: LegacyReferences.java
import kotlin.jvm.internal.FunctionReference;
import kotlin.jvm.internal.PropertyReference0Impl;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KCallable;
import kotlin.reflect.KDeclarationContainer;

public class LegacyReferences {
    public static KCallable<?> function() {
        return new FunctionReference(1) {
            @Override
            public KDeclarationContainer getOwner() {
                return Reflection.getOrCreateKotlinClass(J.class);
            }

            @Override
            public String getName() {
                return "foo";
            }

            @Override
            public String getSignature() {
                return "foo()V";
            }
        };
    }

    public static KCallable<?> property() {
        return new PropertyReference1() {
            @Override
            public KDeclarationContainer getOwner() {
                return Reflection.getOrCreateKotlinClass(J.class);
            }

            @Override
            public String getName() {
                return "property";
            }

            @Override
            public String getSignature() {
                return "getProperty()I";
            }

            @Override
            public Object get(Object receiver) {
                return 0;
            }
        };
    }

    public static KCallable<?> localFunction() {
        return new FunctionReference(0) {
            @Override
            public KDeclarationContainer getOwner() {
                return null;
            }

            @Override
            public String getName() {
                return "local";
            }

            @Override
            public String getSignature() {
                return "local()V";
            }
        };
    }

    public static PropertyReference0Impl localDelegatedProperty() {
        return new PropertyReference0Impl(Reflection.getOrCreateKotlinClass(J.class), "local", "<v#0>");
    }
}

// FILE: test.kt
import kotlin.reflect.*
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(J::class, LegacyReferences.function().container)
    assertEquals(J::class, LegacyReferences.property().container)
    assertEquals(null, LegacyReferences.localFunction().container)
    assertEquals(null, LegacyReferences.localDelegatedProperty().container)
    return "OK"
}
