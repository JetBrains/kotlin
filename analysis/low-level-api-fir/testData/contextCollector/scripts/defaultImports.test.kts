// FULL_JDK
// STDLIB_JDK8
// SCRIPT_DEFAULT_IMPORTS: kotlin.streams.asSequence

import java.util.stream.Stream

Stream.of("foo").asSequence().apply {
    <expr>contains("bar")</expr>
}
