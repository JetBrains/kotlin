public abstract class A /* A*/<T extends A<@org.jetbrains.annotations.NotNull() T>>  extends B<@org.jetbrains.annotations.NotNull() java.util.Collection<? extends T>> implements C<T> {
  public  A();//  .ctor()

  public class Inner /* A.Inner*/<D>  extends B<@org.jetbrains.annotations.NotNull() java.util.Collection<? extends T>> implements C<D> {
    public  Inner();//  .ctor()
  }

  public final class Inner2 /* A.Inner2*/<X>  extends A.Inner<X> implements C<X> {
    public  Inner2();//  .ctor()
  }
}
