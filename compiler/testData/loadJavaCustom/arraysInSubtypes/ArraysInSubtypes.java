package test;

import java.util.List;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface ArraysInSubtypes {
    interface Super {
        CharSequence[] array();
        List<? extends CharSequence[]> listOfArray();

        Object[] objArray();
    }

    interface Sub<T> extends Super {
        @ExpectLoadError("Return type is not a subtype of overridden method. To fix it, add annotation with Kotlin signature to super method with type Array<CharSequence>? replaced with Array<out CharSequence>? in return type")
        String[] array();

        @ExpectLoadError("Return type is not a subtype of overridden method. To fix it, add annotation with Kotlin signature to super method with type Array<CharSequence>? replaced with Array<out CharSequence>? in return type")
        List<? extends String[]> listOfArray();

        @ExpectLoadError("Return type is not a subtype of overridden method. To fix it, add annotation with Kotlin signature to super method with type Array<Any>? replaced with Array<out Any>? in return type")
        T[] objArray();
    }
}