// JSPECIFY_STATE: strict
// ISSUE: KT-65719

// FILE: MessageOrBuilder.java
public interface MessageOrBuilder {
    Object o();
}

// FILE: GeneratedMessage.java
public class GeneratedMessage implements MessageOrBuilder {
    @Override
    public Object o() {
        return null;
    }
}

// FILE: FooOrBuilder.java
public interface FooOrBuilder extends MessageOrBuilder {}

// FILE: Foo.java
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Foo extends GeneratedMessage implements FooOrBuilder {}

// FILE: main.kt
fun main() {
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Any..kotlin.Any?)")!>Foo().o()<!>
}
