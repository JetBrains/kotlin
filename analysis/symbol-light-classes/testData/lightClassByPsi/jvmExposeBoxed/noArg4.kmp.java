@<error>()
@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class IntWrapper /* IntWrapper*/ {
  private final int i;

  public final int getI();//  getI()
}

public final class RegularClassWithValueConstructor /* RegularClassWithValueConstructor*/ {
  private final int property;

  public  RegularClassWithValueConstructor();//  .ctor()

  public  RegularClassWithValueConstructor(int);//  .ctor(int)

  public final int getProperty();//  getProperty()
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class RegularClassWithValueConstructorAndAnnotation /* RegularClassWithValueConstructorAndAnnotation*/ {
  private final int property;

  @<error>()
  public  RegularClassWithValueConstructorAndAnnotation();//  .ctor()

  @<error>()
  public  RegularClassWithValueConstructorAndAnnotation(int);//  .ctor(int)

  public final int getProperty();//  getProperty()
}
