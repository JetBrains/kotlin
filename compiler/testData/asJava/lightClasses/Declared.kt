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
}

// Modality
abstract class Abstract
open class Open
final class Final

// Special
annotation class Annotation
enum class Enum
trait Trait

// Deprecation
deprecated("") class Deprecated
jet.deprecated("") class DeprecatedFQN
[deprecated("")] class DeprecatedWithBrackets
[jet.deprecated("")] class DeprecatedWithBracketsFQN

// Generic
class Generic1<T>
class Generic2<A, B>

