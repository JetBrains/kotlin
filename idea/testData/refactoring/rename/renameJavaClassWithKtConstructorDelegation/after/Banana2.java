import java.util.function.Supplier;

public class Banana2 {
    final Supplier<String> f;

    Banana2(Supplier<String> f) {this.f = f;}

    Banana2() {this(() -> "Default");}

    void goCrazy() {
        System.out.println(f.get());
    }
}