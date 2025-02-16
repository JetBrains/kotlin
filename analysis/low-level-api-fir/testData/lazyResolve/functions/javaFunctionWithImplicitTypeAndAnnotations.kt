// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: defaultMethod
// FILE: main.kt

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: Int)

const val firstConst = 1
const val secondConst = 2

interface BaseKotlinInterface<T> {
    fun defaultMethod(p: List<T>)/*: List<T>*/ = p;
}

class LeafKotlin<caret>Class : JavaExtension

// FILE: JavaExtension.java
import java.util.List;

import static MainKt.firstConst;
import static MainKt.secondConst;

public interface JavaExtension extends BaseKotlinInterface<@Anno(firstConst + secondConst) String> {
    @Override default List<@Anno(firstConst + secondConst) String> defaultMethod(List<@Anno(firstConst + secondConst) String> p) { return null; }
}
