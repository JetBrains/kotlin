package test

class NoModifiers

// Visibility
public class Public
private class Private
internal class Internal
class Outer {
    public class Public
    protected class Protected
    private class Private
    internal class Internal

    inner class Inner
}

// Modality
abstract class Abstract
open class Open
final class Final

// Special
annotation class Annotation
enum class Enum
interface Trait

// Deprecation
@Deprecated("") class DeprecatedClass
@kotlin.Deprecated("") class DeprecatedFQN
@kotlin. Deprecated /**/ ("") class DeprecatedFQNSpaces
@[Deprecated("")] class DeprecatedWithBrackets
@[kotlin.Deprecated("")] class DeprecatedWithBracketsFQN
@[kotlin
./**/Deprecated  ("")] class DeprecatedWithBracketsFQNSpaces

// Generic
class Generic1<T>
class Generic2<A, B>

