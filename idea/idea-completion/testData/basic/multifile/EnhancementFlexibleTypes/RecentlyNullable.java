package androidx.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;

@Target({ElementType.PARAMETER, ElementType.TYPE})
public @interface RecentlyNullable {
}
