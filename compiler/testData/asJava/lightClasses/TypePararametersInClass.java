public abstract class A <T extends A<T>> extends B<java.util.Collection<? extends T>> implements C<T> {
    public A() { /* compiled code */ }

    public class Inner <D> extends B<java.util.Collection<? extends T>> implements C<D> {
        public Inner() { /* compiled code */ }
    }


public class Inner /* A.Inner*/<D>  extends B<java.util.Collection<? extends T>> implements C<D> {
  public  Inner();//  .ctor()

}public final class Inner2 /* A.Inner2*/<X>  extends A.Inner<X> implements C<X> {
  public  Inner2();//  .ctor()

}}
