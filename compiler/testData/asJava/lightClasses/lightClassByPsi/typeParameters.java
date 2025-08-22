public abstract interface I /* I*/ {
}

public abstract interface I2 /* I2*/<T>  {
}

public abstract interface I3 /* I3*/<T extends @org.jetbrains.annotations.NotNull() I>  {
}

public abstract interface I4 /* I4*/<T extends @org.jetbrains.annotations.Nullable() I>  {
}

public abstract interface I5 /* I5*/<Self extends @org.jetbrains.annotations.NotNull() I5<@org.jetbrains.annotations.NotNull() Self>>  {
}

public abstract interface I6 /* I6*/<Self extends @org.jetbrains.annotations.Nullable() I6<Self>>  {
}

public abstract interface I7 /* I7*/<Self extends @org.jetbrains.annotations.Nullable() I7<@org.jetbrains.annotations.Nullable() Self>>  {
}

public abstract interface I8 /* I8*/<Self extends @org.jetbrains.annotations.NotNull() I8<@org.jetbrains.annotations.Nullable() Self>>  {
}
