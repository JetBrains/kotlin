class A {
    // Kind
    annotation class Annotation
    enum class Enum
    interface Trait {
        fun boo() {}
    }

    // Modality
    open class OpenStaticClass
    class FinalStaticClass
    abstract class AbstractStaticClass

    open inner class OpenInnerClass
    inner class FinalInnerClass
    abstract inner class AbstractInnerClass

    // Visibility
    private open inner class PrivateClass
    protected open inner class ProtectedClass
    internal open inner class InternalClass
    public open inner class PublicClass
}
