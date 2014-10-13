// FILE: foo/Base.java

package foo;

public interface Base<T> {}

// FILE: foo/HS.java

package foo;

public class HS<T> extends Base<T> {}

// FILE: k.kt

import foo.*;

import java.util.HashSet
fun <T, C: Base<T>> convert(src: HS<T>, dest: C): C = throw Exception("$src $dest")

fun test(l: HS<Int>) {
    //todo should be inferred
    val r = <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>convert<!>(l, <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>HS<!>())
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>r<!>: Int
}