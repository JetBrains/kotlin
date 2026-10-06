public abstract class AbstractCodeMetaInfoTest /* test.AbstractCodeMetaInfoTest*/ {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() test.AbstractParent> getConfigurations();//  getConfigurations()

  public  AbstractCodeMetaInfoTest();//  .ctor()
}

public abstract class AbstractLineMarkerCodeMetaInfoTest /* test.AbstractLineMarkerCodeMetaInfoTest*/ extends test.AbstractCodeMetaInfoTest {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() test.ConcreteTwo> getConfigurations();//  getConfigurations()

  public  AbstractLineMarkerCodeMetaInfoTest();//  .ctor()
}

public abstract class AbstractParent /* test.AbstractParent*/ {
  public  AbstractParent();//  .ctor()
}

public final class ConcreteOne /* test.ConcreteOne*/ extends test.AbstractParent {
  public  ConcreteOne();//  .ctor()
}

public final class ConcreteTwo /* test.ConcreteTwo*/ extends test.AbstractParent {
  public  ConcreteTwo();//  .ctor()
}
