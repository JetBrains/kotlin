import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

interface ClassifierNamePolicy {
    object SOURCE_CODE_QUALIFIED : ClassifierNamePolicy
}

interface KotlinType

class A(val isLocked: Boolean) {
    private fun <T> property(initialValue: T): ReadWriteProperty<A, T> {
        return Delegates.vetoable(initialValue) { _, _, _ ->
            if (isLocked) {
                throw IllegalStateException("Cannot modify readonly DescriptorRendererOptions")
            }
            else {
                true
            }
        }
    }

    var classifierNamePolicy: ClassifierNamePolicy by property(ClassifierNamePolicy.SOURCE_CODE_QUALIFIED)
    // getter has INAPPLICABLE diagnostic, see dump

    var typeNormalizer by <!INAPPLICABLE_CANDIDATE!>property<!><(KotlinType) -> KotlinType>({ <!UNRESOLVED_REFERENCE!>it<!> })
}
