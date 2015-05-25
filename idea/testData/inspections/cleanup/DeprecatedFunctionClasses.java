import kotlin.Function0;
import kotlin.Function1;
import kotlin.KotlinPackage;
import kotlin.ExtensionFunction0;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class DeprecatedFunctionClasses {
    void f1(Function0 f) {
        f.invoke();
    }

    void f2() {
        KotlinPackage.map(new int[]{}, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer integer) {
                return null;
            }
        });
    }

    void f3(Function2<String, String, String> f) {
    }

    void f4(kotlin.Function0<Unit> g) {
    }

    void f5(kotlin.jvm.functions.Function1 f) {
    }

    void f6(ExtensionFunction0<String, Integer> e) {
    }
}
