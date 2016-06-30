// FILE: B.java
public interface B<T1, T2> {
    double put(int x, double y);
    T2 put(T1 x, T2 y);
}
// FILE: A.java
import java.util.HashMap;

public class A extends HashMap<Integer, Double> implements B<Integer, Double> {
    public double put(int x, double y) {
        return 1.0;
    }

    @Override
    public Double put(Integer key, Double value) {
        return super.put(key, value);
    }
}
// FILE: main.kt
fun test(){
    val o = A()
    o.put(1, 2.0)
}
