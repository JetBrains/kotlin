public abstract interface A /* A*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Class<?> @org.jetbrains.annotations.NotNull() [] kClass();//  kClass()
}

public abstract interface B /* B*/<T, R>  {
}

@Ann(kClass = {})
public abstract interface Test /* Test*/ {
}
