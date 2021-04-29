// INTENTION_TEXT: Remove 'private' modifier
// COMPILER_ARGUMENTS: -Xexplicit-api=strict
public class Test {
    public var foo: String = ""
        <caret>private set
}