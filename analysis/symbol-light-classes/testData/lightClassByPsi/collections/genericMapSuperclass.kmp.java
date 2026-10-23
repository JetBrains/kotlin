public abstract class AbstractGetOrDefaultMap /* test.AbstractGetOrDefaultMap*/<K>  extends kotlin.collections.AbstractMap<K, @org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String getOrDefault(K, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getOrDefault(K, @org.jetbrains.annotations.NotNull() java.lang.String)

  public  AbstractGetOrDefaultMap();//  .ctor()
}

public abstract class BelowAbstractGetOrDefaultMap /* test.BelowAbstractGetOrDefaultMap*/ extends test.AbstractGetOrDefaultMap<@org.jetbrains.annotations.NotNull() java.lang.String> {
  public  BelowAbstractGetOrDefaultMap();//  .ctor()
}

public abstract class BelowDirectMutableMap /* test.BelowDirectMutableMap*/ extends test.DirectMutableMap<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String> {
  public  BelowDirectMutableMap();//  .ctor()
}

public abstract class BelowDirectStringMutableMap /* test.BelowDirectStringMutableMap*/ extends test.DirectStringMutableMap {
  public  BelowDirectStringMutableMap();//  .ctor()
}

public abstract class BelowGenericValueMap /* test.BelowGenericValueMap*/ extends test.GenericValueMap<@org.jetbrains.annotations.NotNull() java.lang.String> {
  public  BelowGenericValueMap();//  .ctor()
}

public abstract class BelowStringMap /* test.BelowStringMap*/ extends test.StringMap {
  public  BelowStringMap();//  .ctor()
}

public abstract class DirectMutableMap /* test.DirectMutableMap*/<K, V>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMutableMap {
  public  DirectMutableMap();//  .ctor()
}

public abstract class DirectStringMutableMap /* test.DirectStringMutableMap*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMutableMap {
  public  DirectStringMutableMap();//  .ctor()
}

public abstract class GenericKeyMutableMap /* test.GenericKeyMutableMap*/<K>  extends kotlin.collections.AbstractMutableMap<K, @org.jetbrains.annotations.NotNull() java.lang.String> {
  public  GenericKeyMutableMap();//  .ctor()
}

public abstract class GenericMap /* test.GenericMap*/<K, V>  extends kotlin.collections.AbstractMap<K, V> {
  public  GenericMap();//  .ctor()
}

public abstract class GenericValueMap /* test.GenericValueMap*/<V>  extends kotlin.collections.AbstractMap<@org.jetbrains.annotations.NotNull() java.lang.String, V> {
  public  GenericValueMap();//  .ctor()
}

public abstract class IntMap /* test.IntMap*/ extends kotlin.collections.AbstractMap<@org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer> {
  public  IntMap();//  .ctor()
}

public abstract class IntMutableMap /* test.IntMutableMap*/ extends kotlin.collections.AbstractMutableMap<@org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer> {
  public  IntMutableMap();//  .ctor()
}

public final class MapObject /* test.MapObject*/ extends kotlin.collections.AbstractMap<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String> {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() test.MapObject INSTANCE;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>> getEntries();//  getEntries()

  private  MapObject();//  .ctor()
}

public abstract class OverridingMap /* test.OverridingMap*/ extends kotlin.collections.AbstractMap<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getOrDefault(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)

  public  OverridingMap();//  .ctor()
}

public abstract class StringMap /* test.StringMap*/ extends kotlin.collections.AbstractMap<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String> {
  public  StringMap();//  .ctor()
}

public abstract class StringMutableMap /* test.StringMutableMap*/ extends kotlin.collections.AbstractMutableMap<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String> {
  public  StringMutableMap();//  .ctor()
}
