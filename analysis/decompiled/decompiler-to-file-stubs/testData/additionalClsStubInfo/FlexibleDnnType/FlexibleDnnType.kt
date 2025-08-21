// FIR_IDENTICAL
// KNM_K2_IGNORE
// KNM_FE10_IGNORE

abstract class FlexibleDnnType<T> {
    abstract val block: () -> T

    val propertyWithFlexibleDnnImplicitType = JavaClass.wrap(JavaClass.wrap<String, T> { _ -> block.invoke() })
}
