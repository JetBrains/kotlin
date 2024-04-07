public abstract interface A /* A*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  public abstract java.lang.Class<?>[] kClass();//  kClass()
}

public abstract interface B /* B*/<T, R>  {
}

@Ann(kClass = {A.class, A.class, A.class, B.class, B.class})
public abstract interface Test /* Test*/ {
}

public final class TypealiasInAnnotationKt /* TypealiasInAnnotationKt*/ {
}
