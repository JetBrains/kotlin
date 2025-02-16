// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: defaultMethod
// FILE: main.kt

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

const val firstConst = 1
const val secondConst = 2

interface BaseKotlinInterface<T> {
    fun defaultMethod(p: List<T>)/*: List<T>*/ = p;
}

class LeafKotlin<caret>Class : JavaExtension<@Anno(firstConst + secondConst) String>

// FILE: JavaExtension.java
import java.util.List;

public interface JavaExtension <T> extends BaseKotlinInterface<T> {
    @Override default List<T> defaultMethod(List<T> p) { return null; }
}
