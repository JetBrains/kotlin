// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: defaultMethod
// FILE: main.kt

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: Int)

const val firstConst = 1
const val secondConst = 2

interface BaseKotlinInterface {
    fun <T> defaultMethod(p: List<T>)/*: List<T>*/ = p;
}

class LeafKotlin<caret>Class : JavaExtension

// FILE: JavaExtension.java
import java.util.List;

import static MainKt.firstConst;
import static MainKt.secondConst;

public interface JavaExtension extends BaseKotlinInterface {
    @Override default <T extends List<@Anno(firstConst + secondConst) String>> List<@Anno(firstConst + secondConst) String> defaultMethod(List<@Anno(firstConst + secondConst) String> p) { return null; }
}
