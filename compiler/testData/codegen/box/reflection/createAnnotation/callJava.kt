// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public interface J {
    @interface NoParams {}

    @interface OneDefault {
        String foo() default "foo";
    }

    @interface OneDefaultValue {
        String value() default "value";
    }

    @interface OneNonDefault {
        String foo();
    }

    @interface OneNonDefaultValue {
        String value();
    }

    @interface TwoParamsOneDefault {
        String string();
        Class<?> clazz() default Object.class;
    }

    @interface TwoParamsOneValueOneDefault {
        String value();
        Class<?> clazz() default Object.class;
    }

    @interface TwoNonDefaults {
        String string();
        Class<?> clazz();
    }

    @interface ManyDefaults {
        int i() default 0;
        String s() default "";
        double d() default 3.14;
    }
}

// FILE: K.kt

import J.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals
import kotlin.test.assertFails

inline fun <reified T : Annotation> create(vararg args: Any?): T =
        T::class.constructors.single().call(*args)

fun box(): String {
    create<NoParams>()

    assertFails { create<OneDefault>() }
    assertFails { create<OneDefault>("") }
    assertFails { create<OneDefault>("", "") }

    assertFails { create<OneDefaultValue>() }
    create<OneDefaultValue>("")
    assertFails { create<OneDefaultValue>("", "") }

    assertFails { create<OneNonDefault>() }
    assertFails { create<OneNonDefault>("") }

    assertFails { create<OneNonDefaultValue>() }
    create<OneNonDefaultValue>("")

    assertFails { create<TwoParamsOneDefault>() }
    assertFails { create<TwoParamsOneDefault>("") }
    assertFails { create<TwoParamsOneDefault>("", Any::class) }
    assertFails { create<TwoParamsOneDefault>(Any::class, "") }

    assertFails { create<TwoParamsOneValueOneDefault>() }
    assertFails { create<TwoParamsOneValueOneDefault>("") }
    assertFails { create<TwoParamsOneValueOneDefault>("", Any::class) }
    assertFails { create<TwoParamsOneValueOneDefault>(Any::class, "") }

    assertFails { create<TwoNonDefaults>("", Any::class) }
    assertFails { create<TwoNonDefaults>(Any::class, "") }

    assertFails { create<ManyDefaults>() }
    assertFails { create<ManyDefaults>(42, "Fail", 2.72) }

    return "OK"
}
